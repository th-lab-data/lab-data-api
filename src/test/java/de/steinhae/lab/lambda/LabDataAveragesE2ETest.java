package de.steinhae.lab.lambda;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.lambda.runtime.Context;

public class LabDataAveragesE2ETest {

    private LabDataAggregates handler = new LabDataAggregates();

//    @Test
    public void testAverages() throws IOException {
        OutputStream os = new ByteArrayOutputStream(100);

        Context ctx = Mockito.mock(Context.class);
        Mockito.when(ctx.getLogger()).thenReturn(System.out::println);

        String input = "{}";
        InputStream is = new ByteArrayInputStream(input.getBytes());
        handler.handleRequest(is, os, ctx);
    }
}
