/*
  INTERFAZ DE USUARIO (CLI) — SISTEMA DE TRANSMISIÓN HAMMING

  Arquitectura de Computadores — Canal de Comunicación Digital
 
  Menú interactivo paso a paso:
    Paso 1 Configuración del código Hamming
    Paso 2 Carga de datos (texto o archivo)
    Paso 3 Codificación Hamming
    Paso 4 Configuración del canal con ruido
    Paso 5 Decodificación y corrección
    Paso 6 Reporte de métricas
    Paso 7 Exportación de resultados (archivo recuperado, CSV)
 */
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            ejecutar();
        } catch (Exception e) {
            System.out.println("\n  X Error inesperado: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void ejecutar() {
        limpiarPantalla();
        printBanner();

        // PASO 1: Configuración del código Hamming
        HammingCore hamming = seleccionarHamming();
        pausa();


        // PASO 2: Carga de datos
        limpiarPantalla();
        printBanner();
        Transmisor tx = seleccionarFuente(hamming);
        pausa();

        // CODIFICACIÓN
        limpiarPantalla();
        printBanner();
        System.out.println("\n" + "─".repeat(72));
        System.out.println("  CODIFICACIÓN HAMMING");
        System.out.println("─".repeat(72));
        System.out.print("  Codificando datos... ");

        tx.encode();
        int totalDataBits = tx.getTotalDataBits();
        int totalBlocks = tx.getNumBlocks();
        int totalChannelBits = tx.getTotalChannelBits();

        System.out.println("CORRECTO");
        System.out.println("  - " + totalBlocks + " bloques generados");
        System.out.println("  - " + totalDataBits + " bits de datos ->" + totalChannelBits + " bits transmitidos");
        System.out.printf("  - Eficiencia: %.2f%%\n", hamming.getEfficiency() * 100);
        pausa();


        // PASO 4: Configuración del ruido

        limpiarPantalla();
        printBanner();
        Object[] ruidoResult = seleccionarRuido(tx.getEncodedBlocks(), hamming);
        int[][] noisyBlocks = (int[][]) ruidoResult[0];
        double ber = (double) ruidoResult[1];
        String noiseMode = (String) ruidoResult[2];
        int errorsInjected = (int) ruidoResult[3];
        pausa();


        // DECODIFICACIÓN

        limpiarPantalla();
        printBanner();
        System.out.println("\n" + "─".repeat(72));
        System.out.println("  DECODIFICACIÓN Y CORRECCIÓN");
        System.out.println("─".repeat(72));
        System.out.print("  Decodificando bloques... ");

        Receptor rx = new Receptor(hamming);
        byte[] decoded = rx.decode(noisyBlocks, totalDataBits);

        System.out.println("CORRECTO");
        System.out.println("  - Errores detectados:        " + rx.getTotalDetected());
        System.out.println("  - Errores corregidos (1-bit): " + rx.getTotalCorrected());
        System.out.println("  - Errores fatales (>1-bit):   " + rx.getTotalFatal());

        if (rx.getTotalFatal() > 0) {
            System.out.println();
            System.out.println("   Se detectaron errores fatales. El algoritmo Hamming estándar");
            System.out.println("  no puede corregir múltiples errores en un mismo bloque.");
        }
        pausa();

        // PASO 6: Reporte de métricas

        limpiarPantalla();
        printBanner();
        int[] stats = {rx.getTotalDetected(), rx.getTotalCorrected(), rx.getTotalFatal()};
        String report = AnalizadorMetricas.generateReport(
            tx.getOriginalBytes(), decoded,
            totalDataBits, totalBlocks,
            errorsInjected, stats,
            hamming, ber, noiseMode,
            tx.getOriginalPath()
        );
        System.out.println("\n" + report);

        menuDetalleBloque(rx, noisyBlocks);


        // EXPORTACIONES
        System.out.println("\n" + "─".repeat(72));
        System.out.println("  EXPORTACIÓN DE RESULTADOS");
        System.out.println("─".repeat(72));

        // Archivo recuperado (solo si la fuente era un archivo)
        if (!tx.getOriginalPath().equals("<texto>")) {
            exportarArchivoRecuperado(decoded, tx.getOriginalPath());
        }

        // CSV
        exportarCSV(hamming, noiseMode, ber, errorsInjected, stats, decoded, tx, totalBlocks);

        System.out.println();
        System.out.println("  " + "═".repeat(68));
        System.out.println("  Sistema finalizado. ¡Gracias por usar el simulador Hamming!");
        System.out.println("  " + "═".repeat(68));
        System.out.println();
    }

    // MENÚS AUXILIARES


    private static void limpiarPantalla() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Si falla, simplemente imprime saltos de línea
            System.out.print("\n".repeat(50));
        }
    }

    private static void printBanner() {
        System.out.println("------------------------------------------------------------------------");
        System.out.println("       SISTEMA DE TRANSMISIÓN HAMMING CON SIMULACIÓN DE RUIDO           ");
        System.out.println("    Arquitectura de Computadores - Canal de Comunicación Digital        ");
        System.out.println("------------------------------------------------------------------------");
    }

    private static void pausa() {
        System.out.print("\n  Presione Enter para continuar...");
        scanner.nextLine();
    }

    private static String input(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }


    // PASO 1: Selección de Hamming


    private static HammingCore seleccionarHamming() {
        System.out.println("\n" + "─".repeat(72));
        System.out.println("           PASO 1 CONFIGURACIÓN DEL CÓDIGO HAMMING ");
        System.out.println("─".repeat(72));
        System.out.println("  Seleccione el código Hamming:");
        System.out.println();
        System.out.println("    1) Hamming(7,4)   -  4 bits datos, 3 paridad  (n = 57.1%)");
        System.out.println("    2) Hamming(15,11) - 11 bits datos, 4 paridad  (n = 73.3%)");
        System.out.println("    3) Hamming(31,26) - 26 bits datos, 5 paridad  (n = 83.9%)");
   //      System.out.println("    4) Personalizado  - especificar bits de datos");

        String opt = input("\n  Opción 1: ");
        if (opt.isEmpty()) opt = "1";

        int k;
        switch (opt) {
            case "1": k = 4;  break;
            case "2": k = 11; break;
            case "3": k = 26; break;
            case "4":
                try {
                    k = Integer.parseInt(input("  Bits de datos por bloque 4: "));
                    if (k < 1) { k = 4; System.out.println("  - Valor inválido, usando 4"); }
                } catch (NumberFormatException e) {
                    k = 4;
                    System.out.println("  - Entrada inválida, usando 4");
                }
                break;
            default:
                k = 4;
        }

        HammingCore hamming = new HammingCore(k);
        System.out.printf("\n  - Código: %s\n", hamming);
        return hamming;
    }

    // PASO 2: Carga de datos


    private static Transmisor seleccionarFuente(HammingCore hamming) {
        System.out.println("\n" + "─".repeat(72));
        System.out.println("         PASO 2 CARGA DE DATOS");
        System.out.println("─".repeat(72));
        System.out.println("  Seleccione el origen de datos:");
        System.out.println("    1) Texto ingresado por teclado");
        System.out.println("    2) Archivo (imagen, audio, binario)");

        String opt = input("\n  Opción 1: ");
        if (opt.isEmpty()) opt = "1";

        Transmisor tx = new Transmisor(hamming);

        if (opt.equals("1")) {
            String text = input("  Ingrese el texto a transmitir: ");
            if (text.isEmpty()) {
                text = "Hola Mundo — Prueba del Sistema Hamming 2024";
                System.out.println("  - Usando texto por defecto: \"" + text + "\"");
            }
            tx.loadText(text);
            int size = tx.getOriginalBytes().length;
            System.out.println("  - " + text.length() + " caracteres, " + size + " bytes (" + (size * 8) + " bits)");
        } else {
            while (true) {
                String path = input("  Ruta del archivo: ");
                try {
                    tx.loadFile(path);
                    int size = tx.getOriginalBytes().length;
                    System.out.printf("  - %s, %d bytes (%d bits, %.1f KB)%n",
                        new java.io.File(path).getName(), size, size * 8, size / 1024.0);
                    break;
                } catch (IOException e) {
                    System.out.println("  X Error: archivo no encontrado en '" + path + "'");
                    System.out.print("  ¿Intentar de nuevo? (S/n): ");
                    String again = scanner.nextLine().trim().toLowerCase();
                    if (again.equals("n") || again.equals("no")) {
                        System.out.println("  - Usando texto por defecto.");
                        tx.loadText("Hola Mundo — Prueba Hamming");
                        break;
                    }
                }
            }
        }

        return tx;
    }


    // PASO 3: Configuración de ruido

    private static Object[] seleccionarRuido(int[][] blocks, HammingCore hamming) {
        System.out.println("\n" + "─".repeat(72));
        System.out.println("      PASO 3 CONFIGURACIÓN DEL CANAL CON RUIDO  ");
        System.out.println("─".repeat(72));
        System.out.println("  Seleccione el modo de inyección de ruido:");
        System.out.println();
        System.out.println("    1) Probabilidad (BER)      - |cada bit tiene probabilidad p de errar");
        System.out.println("    2) Errores exactos totales - |se inyectan EXACTAMENTE N errores en total");
        System.out.println("    3) Errores por bloque      - |se inyectan EXACTAMENTE M errores en CADA bloque");
       

        String opt = input("\n  Opción 1: ");
        if (opt.isEmpty()) opt = "1";

        CanalConRuido canal = new CanalConRuido();
        String noiseMode = "";
        double ber = 0.0;
        int[][] noisy;

        try {
            switch (opt) {
                case "1": {
                    String berStr = input("  BER (probabilidad de error por bit, ej: 0.01 = 1%) [0.01]: ");
                    if (berStr.isEmpty()) berStr = "0.01";
                    ber = Double.parseDouble(berStr);
                    ber = Math.max(0.0, Math.min(1.0, ber));
                    noiseMode = String.format("BER = %.6f", ber);
                    noisy = canal.injectBER(blocks, ber);
                    break;
                }
                case "2": {
                    String nStr = input("  Número total de errores a inyectar [1]: ");
                    if (nStr.isEmpty()) nStr = "1";
                    int n = Integer.parseInt(nStr);
                    n = Math.max(0, n);
                    noiseMode = n + " errores totales";
                    noisy = canal.injectExact(blocks, n);
                    break;
                }
                case "3": {
                    String nStr = input("  Errores por bloque [1]: ");
                    if (nStr.isEmpty()) nStr = "1";
                    int n = Integer.parseInt(nStr);
                    n = Math.max(0, n);
                    noiseMode = n + " errores/bloque";
                    noisy = canal.injectPerBlock(blocks, n);
                    break;
                }
                default:
                    ber = 0.01;
                    noiseMode = "BER = 0.010000";
                    noisy = canal.injectBER(blocks, ber);
            }
        } catch (NumberFormatException e) {
            System.out.println("  -> Entrada inválida. Usando BER = 0.01.");
            ber = 0.01;
            noiseMode = "BER = 0.010000";
            noisy = canal.injectBER(blocks, ber);
        }

        int errInj = canal.getTotalErrorsInjected();
        int totalChannelBits = blocks.length * hamming.getN();
        double errorRate = totalChannelBits > 0 ? (double) errInj / totalChannelBits : 0;

        System.out.printf("\n  - %d errores inyectados sobre %d bits%n", errInj, totalChannelBits);
        System.out.printf("  - Tasa de error real: %.6f (%.4f%%)%n", errorRate, errorRate * 100);

        return new Object[]{noisy, ber, noiseMode, errInj};
    }


    // Detalle por bloque

    private static void menuDetalleBloque(Receptor rx, int[][] noisyBlocks) {
        System.out.print("\n  ¿Ver detalle de errores por bloque? (s/N): ");
        String resp = scanner.nextLine().trim().toLowerCase();
        if (!resp.equals("s")) return;

        List<Receptor.BlockDetail> details = rx.getBlockDetails();
        System.out.println("\n  Detalle por bloque (mostrando solo bloques con error):");
        System.out.printf("  %6s | %10s | %10s | %s%n", "Bloque", "Estado", "Pos.Error", "Detalle");
        System.out.println("  " + "-".repeat(6) + "-+-" + "-".repeat(10) + "-+-" + "-".repeat(10) + "-+-" + "-".repeat(30));

        int count = 0;
        for (Receptor.BlockDetail d : details) {
            if (!d.detected) continue;
            if (count >= 20) {
                System.out.printf("  ... y %d bloques más con error (mostrando 20)%n",
                    details.size() - count);
                break;
            }
            String estado = d.corrected ? "CORREGIDO" : "FATAL";
            String pos = d.errorPosition > 0 ? String.valueOf(d.errorPosition) : "MÚLTIPLE";
            int[] block = noisyBlocks[d.blockIndex];
            StringBuilder det = new StringBuilder("bloque[" + d.blockIndex + "]: ");
            for (int i = 0; i < Math.min(block.length, 7); i++) {
                det.append(block[i]);
            }
            det.append("...");
            System.out.printf("  %6d | %10s | %10s | %s%n", d.blockIndex, estado, pos, det.toString());
            count++;
        }
        if (count == 0) {
            System.out.println("  (ningún bloque con error)");
        }
    }


    // Exportar archivo recuperado

    private static void exportarArchivoRecuperado(byte[] decoded, String originalPath) {
        System.out.print("\n  ¿Exportar archivo recuperado? (s/N): ");
        String resp = scanner.nextLine().trim().toLowerCase();
        if (!resp.equals("s")) return;

        String ext = "";
        int dotIdx = originalPath.lastIndexOf('.');
        if (dotIdx > 0) ext = originalPath.substring(dotIdx);

        String outPath = "recuperado" + ext;
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(outPath), decoded);
            System.out.println("  - Archivo exportado: " + outPath + " (" + decoded.length + " bytes)");
        } catch (IOException e) {
            System.out.println("  X Error al exportar archivo: " + e.getMessage());
        }
    }


    // Exportar CSV


    private static void exportarCSV(
            HammingCore hamming, String noiseMode, double ber,
            int errorsInjected, int[] stats,
            byte[] decoded, Transmisor tx, int totalBlocks) {

        System.out.print("\n  ¿Exportar métricas a CSV? (s/N): ");
        String resp = scanner.nextLine().trim().toLowerCase();
        if (!resp.equals("s")) return;

        String origHash = AnalizadorMetricas.sha256(tx.getOriginalBytes());
        String decHash = AnalizadorMetricas.sha256(decoded);
        boolean integrityOK = origHash.equals(decHash);

        String csvRow = AnalizadorMetricas.csvRow(
            hamming.getN(), hamming.getK(),
            noiseMode, ber,
            errorsInjected, stats[0], stats[1], stats[2],
            integrityOK,
            tx.getOriginalBytes().length, totalBlocks
        );

        AnalizadorMetricas.exportToCSV("metricas_hamming.csv", csvRow);
        System.out.println("  - Para generar gráficos, importe el CSV en Excel, Google Sheets");
        System.out.println("     o Python (pandas + matplotlib).");
    }
}