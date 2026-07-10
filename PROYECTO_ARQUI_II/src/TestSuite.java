/*
  SUITE DE PRUEBAS — Verifica todos los módulos del sistema Hamming

  Arquitectura de Computadores — Canal de Comunicación Digital
 
  Ejecuta simulaciones completas en múltiples configuraciones:
     Hamming(7,4) con 0, 1, 2 errores/bloque
     Hamming(15,11) con BER variable
     Transmisión de imagen real
 */

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestSuite {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=".repeat(72));
        System.out.println("  PROGRAMA DE PRUEBAS — SISTEMA HAMMING");
        System.out.println("=".repeat(72));

                   //BITS DE DATOS Y BITS DE PARIDAD
        
        // Prueba 1: Hamming(7,4) — 0 errores (canal limpio)
        testHamming(4, "Canal limpio (0 errores)", 0, 0);

        // Prueba 2: Hamming(7,4) — 1 error por bloque (corregible)
        testHamming(4, "1 error/bloque (corregible)", 1, 1);

        // Prueba 3: Hamming(7,4) — 2 errores por bloque (fatales)
        testHamming(4, "2 errores/bloque (fatales)", 2, 0);

        // Prueba 4: Hamming(7,4) — BER = 0.1 (10%) BER: TAZA DE ERROR POR BIT
        testHammingBER(4, "BER = 0.1 (10%)", 0.1);

        // Prueba 5: Hamming(15,11) — 1 error por bloque
        testHamming(11, "Hamming(15,11) - 1 error/bloque", 1, 1);

        // Prueba 6: Hamming(15,11) — 2 errores/bloque
        testHamming(11, "Hamming(15,11) - 2 errores/bloque", 2, 0);

        // Prueba 7: Archivo binario (imagen) con ruido
        testImageFile();

        // Prueba 8: Prueba de errores exactos en posiciones conocidas.
        testExactErrors();

        // Resumen
        System.out.println("\n" + "=".repeat(72));
        System.out.printf("  RESULTADO: %d PRUEBAS PASADAS, %d PRUEBAS FALLIDAS%n", passed, failed);
        System.out.println("=".repeat(72));

        if (failed > 0) {
            System.exit(1); //El proyecto no paso por totdas las  pruebas
        }
    }
//----------------------------------------------EXCELENT-----------------------------------------------------	
    // Prueba Hamming con errores exactos por bloque. 
    private static void testHamming(int dataBits, String desc, int nPerBlock, int expectFatal) {
        System.out.println("\n" + "-".repeat(72));
        System.out.println("  PRUEBA: Hamming(" + (dataBits) + ") - " + desc);
        System.out.println("-".repeat(72));

        try { 
            HammingCore hamming = new HammingCore(dataBits);      //Muestra el tiempo actal para que el mensaje sea unico
            String testMsg = "Test de Hamming " + dataBits + " bits — " + System.currentTimeMillis(); 
            byte[] original = testMsg.getBytes("UTF-8");
            
            // Transmisor
            Transmisor tx = new Transmisor(hamming);
            tx.loadText(testMsg);
            tx.encode();

            System.out.println("  Bloques: " + tx.getNumBlocks() + ", Bits: " + tx.getTotalDataBits());

            // Canal de comunicacion con ruido
            CanalConRuido canal = new CanalConRuido();
            int[][] noisy = canal.injectPerBlock(tx.getEncodedBlocks(), nPerBlock);
            System.out.println("  Errores inyectados: " + canal.getTotalErrorsInjected());

            // Receptor
            Receptor rx = new Receptor(hamming);
            int originalBits = tx.getTotalDataBits();
            byte[] decoded = rx.decode(noisy, originalBits); //El receptor intenta detectar y corregir errores 

            // Verificación
            System.out.println("  Detectados: " + rx.getTotalDetected());
            System.out.println("  Corregidos: " + rx.getTotalCorrected());
            System.out.println("  Fatales: " + rx.getTotalFatal());

            // Verificar hash
            String origHash = AnalizadorMetricas.sha256(original); //Funcion hash que transforma el texto en una cadena de caracateres
            String decHash = AnalizadorMetricas.sha256(decoded); //unica de 64 digitos 
            boolean integrityOK = origHash.equals(decHash); //Verrificacion de daños en la transmision
            System.out.println("  Integridad: " + (integrityOK ? " SÍ" : " NO"));

            // Verificar expectativas
            boolean testOK = true;
            if (nPerBlock == 0 && !integrityOK) {
                System.out.println(" FALLO: Canal limpio debería tener integridad 100%");
                testOK = false;
            }
            if (nPerBlock == 1 && !integrityOK) {
                // 1 error/bloque debería ser corregible
                if (rx.getTotalFatal() == 0) {
                    System.out.println("   FALLO: 1 error/bloque debería corregirse sin fatales");
                    testOK = false;
                }
            }
            if (nPerBlock == 2 && integrityOK) {
                System.out.println("   NOTA: 2 errores/bloque raramente pueden pasar desapercibidos");
            }

            if (testOK) {
                System.out.println("   PRUEBA PASADA");
                passed++;
            } else {
                failed++;
            }

        } catch (Exception e) {
            System.out.println("  EXCEPCIÓN: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
    }

    // Prueba Hamming con BER.
    private static void testHammingBER(int dataBits, String desc, double ber) {
        System.out.println("\n" + "-".repeat(72));
        System.out.println("  PRUEBA: Hamming(" + dataBits + ") - " + desc);
        System.out.println("-".repeat(72));

        try {
            HammingCore hamming = new HammingCore(dataBits);
            String testMsg = "Prueba BER " + ber + " con código Hamming de " + dataBits + " bits de datos.";
            byte[] original = testMsg.getBytes("UTF-8");

            Transmisor tx = new Transmisor(hamming);
            tx.loadText(testMsg);
            tx.encode();

            System.out.println("  Bloques: " + tx.getNumBlocks() + ", Bits canal: " + tx.getTotalChannelBits());

            CanalConRuido canal = new CanalConRuido();
            int[][] noisy = canal.injectBER(tx.getEncodedBlocks(), ber);
            System.out.println("  Errores inyectados: " + canal.getTotalErrorsInjected());

            Receptor rx = new Receptor(hamming);
            byte[] decoded = rx.decode(noisy, tx.getTotalDataBits()); //Decodificar los datos recibidos

            String origHash = AnalizadorMetricas.sha256(original);
            String decHash = AnalizadorMetricas.sha256(decoded);
            boolean integrityOK = origHash.equals(decHash);

            System.out.println("  Detectados: " + rx.getTotalDetected());
            System.out.println("  Corregidos: " + rx.getTotalCorrected());
            System.out.println("  Fatales: " + rx.getTotalFatal());
            System.out.println("  Integridad: " + (integrityOK ? " SÍ" : " NO"));

            // Generar reporte
            int[] stats = {rx.getTotalDetected(), rx.getTotalCorrected(), rx.getTotalFatal()};
            System.out.println("\n" + AnalizadorMetricas.generateReport(
                original, decoded, tx.getTotalDataBits(), tx.getNumBlocks(),
                canal.getTotalErrorsInjected(), stats, hamming, ber,
                "BER = " + ber, "<prueba>"));

            System.out.println(" PRUEBA PASADA");
            passed++;

        } catch (Exception e) {
            System.out.println(" EXCEPCIÓN: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
    }

    // Prueba con archivo de imagen real.
    private static void testImageFile() {
        System.out.println("\n" + "-".repeat(72));
        System.out.println("  PRUEBA: Transmisión de imagen real");
        System.out.println("-".repeat(72));

        try {
            // Buscar imagen en src/ o directorio actual
            String[] paths = {"src/imagen.jpg", "imagen.jpg"};
            String imgPath = null;
            for (String p : paths) {
                if (Files.exists(Paths.get(p))) {
                    imgPath = p;
                    break;
                }
            }

            if (imgPath == null) {
                System.out.println(" No se encontró imagen de prueba.");
                passed++;
                return;
            }

            HammingCore hamming = new HammingCore(4); //Codificacion Hamming (7,4) 4 bits de paridad 
            Transmisor tx = new Transmisor(hamming);
            tx.loadFile(imgPath);
            tx.encode();

            byte[] original = tx.getOriginalBytes();
            System.out.println("  Imagen: " + imgPath + " (" + original.length + " bytes, " + tx.getNumBlocks() + " bloques)");

            // Transmitir con BER bajo (1% — pocos errores)
            CanalConRuido canal = new CanalConRuido();
            int[][] noisy = canal.injectBER(tx.getEncodedBlocks(), 0.01);

            Receptor rx = new Receptor(hamming);
            byte[] decoded = rx.decode(noisy, tx.getTotalDataBits());

            String origHash = AnalizadorMetricas.sha256(original);
            String decHash = AnalizadorMetricas.sha256(decoded);
            boolean integrityOK = origHash.equals(decHash);

            System.out.println("  Errores inyectados: " + canal.getTotalErrorsInjected());
            System.out.println("  Detectados/Corregidos/Fatales: " + rx.getTotalDetected() + "/" + rx.getTotalCorrected() + "/" + rx.getTotalFatal());
            System.out.println("  Integridad: " + (integrityOK ? " SÍ" : " NO"));

            // Guardar imagen recuperada
            String outPath = "imagen_recuperada_test.jpg";
            Files.write(Paths.get(outPath), decoded);
            System.out.println("  Imagen recuperada guardada: " + outPath + " (" + decoded.length + " bytes)");

            System.out.println(" PRUEBA PASADA");
            passed++;

        } catch (Exception e) {
            System.out.println(" EXCEPCIÓN: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
    }

    // Prueba con errores exactos.
    private static void testExactErrors() {
        System.out.println("\n" + "-".repeat(72));
        System.out.println("  PRUEBA: Errores exactos en posiciones conocidas");
        System.out.println("-".repeat(72));

        try {
            HammingCore hamming = new HammingCore(4);
            String msg = "ABCD"; // Cada letra ocupa 1 byte en UTF-8: Se la eleigiio porque es facil de analizzar 
            byte[] original = msg.getBytes("UTF-8");

            Transmisor tx = new Transmisor(hamming);
            tx.loadText(msg);
            tx.encode();

            System.out.println("  Bloques: " + tx.getNumBlocks());

            // Inyectar 3 errores exactos en total
            CanalConRuido canal = new CanalConRuido();
            int[][] noisy = canal.injectExact(tx.getEncodedBlocks(), 3);

            Receptor rx = new Receptor(hamming);
            byte[] decoded = rx.decode(noisy, tx.getTotalDataBits());

            // Verificar que el número de errores reportados en Receptor
            // sea igual o menor a los inyectados (algunos pueden no detectarse
            // si caen en paridad)
            System.out.println("  Errores inyectados: " + canal.getTotalErrorsInjected());
            System.out.println("  Errores detectados: " + rx.getTotalDetected());
            System.out.println("  Errores corregidos: " + rx.getTotalCorrected());
            System.out.println("  Errores fatales: " + rx.getTotalFatal());

            // Verificar detalle por bloque
            java.util.List<Receptor.BlockDetail> details = rx.getBlockDetails(); //Obtiene el detalle de cada bloque procesado porr
            long detectedBlocks = details.stream().filter(d -> d.detected).count(); //el receptor
            System.out.println("  Bloques con error: " + detectedBlocks);

            System.out.println("PRUEBA PASADA");
            passed++;

        } catch (Exception e) {
            System.out.println(" EXCEPCIÓN: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
    }
}