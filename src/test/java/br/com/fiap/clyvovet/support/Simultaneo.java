package br.com.fiap.clyvovet.support;

import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Pedidos soltos na mesma largada, para os testes de repeticao simultanea.
 *
 * <p>Cada thread fica parada na largada ate todas estarem prontas. Sem isso, a
 * primeira terminaria antes de a ultima comecar e o teste viraria um "duas
 * vezes em sequencia" disfarcado.</p>
 */
public final class Simultaneo {

    private Simultaneo() {
    }

    public static List<MockHttpServletResponse> disparar(List<Callable<MockHttpServletResponse>> pedidos)
            throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(pedidos.size());
        CountDownLatch prontas = new CountDownLatch(pedidos.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<MockHttpServletResponse>> emAndamento = new ArrayList<>();
            for (Callable<MockHttpServletResponse> pedido : pedidos) {
                emAndamento.add(threads.submit(() -> {
                    prontas.countDown();
                    largada.await();
                    return pedido.call();
                }));
            }
            prontas.await(10, TimeUnit.SECONDS);
            largada.countDown();

            List<MockHttpServletResponse> respostas = new ArrayList<>();
            for (Future<MockHttpServletResponse> f : emAndamento) {
                respostas.add(f.get(30, TimeUnit.SECONDS));
            }
            return respostas;
        } finally {
            threads.shutdownNow();
        }
    }

    public static List<MockHttpServletResponse> disparar(int vezes, Callable<MockHttpServletResponse> pedido)
            throws Exception {
        List<Callable<MockHttpServletResponse>> pedidos = new ArrayList<>();
        for (int i = 0; i < vezes; i++) {
            pedidos.add(pedido);
        }
        return disparar(pedidos);
    }
}
