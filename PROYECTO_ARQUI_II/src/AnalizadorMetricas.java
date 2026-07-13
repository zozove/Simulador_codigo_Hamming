/*
  MÓDULO ANALIZADOR DE MÉTRICAS — REPORTES Y VERIFICACIÓN DE INTEGRIDAD
  Arquitectura de Computadores — Canal de Comunicación Digital
 
  Genera reportes estructurados con todas las métricas de la simulación:
  1. Configuración del sistema Hamming
  2. Estadísticas de transmisión (bits, bloques, errores)
  3. Estadísticas de corrección (detectados, corregidos, fatales)
  4. Verificación de integridad mediante hash SHA-256
  5. Exportación a CSV para análisis comparativo
 
  La verificación de integridad compara los hashes SHA-256 del archivo
  original y del archivo recuperado. Si coinciden, la transmisión fue
  100% exitosa (todos los errores fueron corregidos o no hubo errores
  que afectaran los datos).
 */

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AnalizadorMetricas {

    /**
      Calcula el hash SHA-256 de un array de bytes.
      SHA-256 es un algoritmo hash criptográfico que produce una huella
      digital de 256 bits (64 caracteres hex). Es computacionalmente
      inviable encontrar dos entradas con el mismo hash.
     
      @param data Datos a hashear
      @return Hash SHA-256 en formato hexadecimal
     */
    public static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    /*
      Calcula el hash MD5 de un array de bytes.
      MD5 produce una huella de 128 bits. Aunque ya no es seguro
      criptográficamente, sigue siendo útil para verificación rápida
      de integridad en contextos no adversariales.
     */
    public static String md5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 no disponible", e);
        }
    }

    // Convierte array de bytes a string hexadecimal.
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    /*
     Genera un reporte completo y formateado para la consola.
        Incluye:
        - Configuración del sistema Hamming
        - Estadísticas de transmisión
        - Estadísticas de corrección
        - Verificación de integridad mediante SHA-256
        - Advertencias si hay errores fatales
     */
    public static String generateReport(
            byte[] originalBytes, byte[] decodedBytes,
            int totalDataBits, int totalBlocks,
            int errorsInjected, int[] stats,
            HammingCore hamming, double ber, String noiseMode,
            String sourcePath) {

        int detected = stats[0];
        int corrected = stats[1];
        int fatal = stats[2];

        String origHash = sha256(originalBytes);
        String decHash = sha256(decodedBytes);
        boolean integrityOK = origHash.equals(decHash);

        int totalChannelBits = totalBlocks * hamming.getN();
        double berReal = totalChannelBits > 0 ? (double) errorsInjected / totalChannelBits : 0;
        double correctedPct = detected > 0 ? (double) corrected / detected * 100 : 0;
        double fatalPct = detected > 0 ? (double) fatal / detected * 100 : 0;

        StringBuilder sb = new StringBuilder();
        String sep = "-" .repeat(72);
        String sub = "-" .repeat(72);

        sb.append(sep).append("\n");
        sb.append("  REPORTE DE MÉTRICAS - SISTEMA HAMMING\n");
        sb.append(sep).append("\n\n");

        sb.append("  ── CONFIGURACIÓN DEL SISTEMA ──\n");
        sb.append(String.format("  Código Hamming:         %s\n", hamming));
        sb.append(String.format("  Bits de paridad (r):    %d\n", hamming.getR()));
        sb.append(String.format("  Redundancia:            %.2f%% (%d/%d)\n",
            (double) hamming.getR() / hamming.getN() * 100, hamming.getR(), hamming.getN()));
        sb.append(String.format("  Eficiencia:             %.2f%%\n", hamming.getEfficiency() * 100));
        sb.append(String.format("  Fuente de datos:        %s\n", sourcePath));
        sb.append(String.format("  Tamaño original:        %d bytes (%d bits)\n",
            originalBytes.length, totalDataBits));
        sb.append(String.format("  Modo de ruido:          %s\n", noiseMode));
        if (ber > 0) {
            sb.append(String.format("  BER objetivo:           %.6f\n", ber));
        }
        sb.append("\n");

        sb.append("  ── ESTADÍSTICAS DE TRANSMISIÓN ──\n");
        sb.append(String.format("  Bits de datos totales:     %d\n", totalDataBits));
        sb.append(String.format("  Bloques transmitidos:      %d\n", totalBlocks));
        sb.append(String.format("  Bits en el canal:          %d\n", totalChannelBits));
        sb.append(String.format("  Errores inyectados:        %d\n", errorsInjected));
        sb.append(String.format("  BER real:                  %.6f (%d/%d)\n",
            berReal, errorsInjected, totalChannelBits));
        sb.append("\n");

        sb.append("  ── ESTADÍSTICAS DE CORRECCIÓN ──\n");
        sb.append(String.format("  Bloques con error:        %d (%.1f%% del total)\n",
            detected, (double) detected / totalBlocks * 100));
        sb.append(String.format("  Errores corregidos (1-bit): %d (%.1f%% de detectados)\n",
            corrected, correctedPct));
        sb.append(String.format("  Errores fatales (>1-bit):   %d (%.1f%% de detectados)\n",
            fatal, fatalPct));
        sb.append("\n");

        sb.append("  ── VERIFICACIÓN DE INTEGRIDAD ──\n");
        sb.append(String.format("  SHA-256 original:       %s\n", origHash));
        sb.append(String.format("  SHA-256 recuperado:     %s\n", decHash));
        sb.append(String.format("  Integridad 100%%:          %s\n", integrityOK ? "SÍ" : "NO"));
        sb.append(String.format("  Tamaño recuperado:      %d bytes\n", decodedBytes.length));

        // Advertencia si hay errores fatales
        if (fatal > 0) {
            sb.append("\n");
            sb.append("  ADVERTENCIA: ERRORES FATALES DETECTADOS\n");
            sb.append("  El código Hamming estándar solo puede CORREGIR 1 error por bloque.\n");
            sb.append("  Con 2 o más errores en un bloque, pueden ocurrir dos cosas:\n");
            sb.append("    a) El síndrome apunta a una posición incorrecta\n");
            sb.append("    b) El síndrome da 0 falsamente (error pasa desapercibido)\n");
            sb.append("  En ambos casos, los datos recuperados NO son idénticos al original.\n");
            sb.append("  Para manejar múltiples errores se requieren códigos más potentes\n");
            sb.append("  (ej. Hamming extendido, BCH, Reed-Solomon, convolucionales).\n");
        }

        sb.append(sep).append("\n");
        return sb.toString();
    }

    /*
      Devuelve el encabezado para el archivo CSV de métricas.
      Permite recopilar múltiples simulaciones para generar
      tablas comparativas y gráficos de rendimiento.
     */
    public static String csvHeader() {
        return "hamming_n,hamming_k,modo_ruido,ber_objetivo,"
             + "errores_inyectados,errores_detectados,"
             + "errores_corregidos,errores_fatales,integridad_ok,"
             + "tamanio_bytes,bloques_totales";
    }

    /*
     Genera una fila CSV con los datos de una simulación.
     */
    public static String csvRow(
            int hammingN, int hammingK,
            String noiseMode, double ber,
            int errorsInjected, int detected,
            int corrected, int fatal,
            boolean integrityOK,
            int sizeBytes, int totalBlocks) {
        return String.format("%d,%d,%s,%.8f,%d,%d,%d,%d,%d,%d,%d",
            hammingN, hammingK, noiseMode, ber,
            errorsInjected, detected, corrected, fatal,
            integrityOK ? 1 : 0, sizeBytes, totalBlocks);
    }

    /*
     Exporta una fila de métricas a un archivo CSV.
     Crea el encabezado si el archivo no existe
     */
    public static void exportToCSV(String csvPath, String csvRowData) {
        try {
            boolean headerNeeded = !new java.io.File(csvPath).exists();
            try (PrintWriter pw = new PrintWriter(new FileWriter(csvPath, true))) {
                if (headerNeeded) {
                    pw.println(csvHeader());
                }
                pw.println(csvRowData);
            }
            System.out.println(" CSV exportado a: " + csvPath);
        } catch (IOException e) {
            System.out.println(" X Error al exportar CSV: " + e.getMessage());
        }
    }
}
