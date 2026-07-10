/*
  MÓDULO RECEPTOR — DECODIFICACIÓN, CORRECCIÓN Y RECONSTRUCCIÓN

  Arquitectura de Computadores — Canal de Comunicación Digital
 
  Recibe los bloques ruidosos del canal, los decodifica aplicando
  corrección de errores Hamming, y reconstruye los datos originales.
 
  Registro detallado por bloque:
   Errores detectados
   Errores corregidos (1-bit)
   Errores fatales (2+ bits, no corregibles)
   Posición de cada error detectado
 
  La reconstrucción final convierte los bits extraídos de cada bloque
  nuevamente a bytes usando el mismo orden MSB first del transmisor.
 */

import java.util.ArrayList;
import java.util.List;

public class Receptor {
    private final HammingCore hamming;
    private int totalDetected;
    private int totalCorrected;
    private int totalFatal;
    private byte[] decodedBytes;
    private int[] decodedBits;
    private final List<BlockDetail> blockDetails;

    // Detalle de la decodificación de un bloque individual
    public static class BlockDetail {
        public final int blockIndex;
        public final int errorPosition; 
        public final boolean detected;
        public final boolean corrected;

        public BlockDetail(int blockIndex, int errorPosition, boolean detected, boolean corrected) {
            this.blockIndex = blockIndex;
            this.errorPosition = errorPosition;
            this.detected = detected;
            this.corrected = corrected;
        }
    }

    public Receptor(HammingCore hamming) {
        this.hamming = hamming;
        this.totalDetected = 0;
        this.totalCorrected = 0;
        this.totalFatal = 0;
        this.decodedBytes = new byte[0];
        this.decodedBits = new int[0];
        this.blockDetails = new ArrayList<>();
    }

    // Getters
    public int getTotalDetected() { return totalDetected; }
    public int getTotalCorrected() { return totalCorrected; }
    public int getTotalFatal() { return totalFatal; }
    public byte[] getDecodedBytes() { return decodedBytes; }
    public int[] getDecodedBits() { return decodedBits; }
    public List<BlockDetail> getBlockDetails() { return blockDetails; }

    /*
      Decodifica todos los bloques ruidosos, corrigiendo errores.
     
      Por cada bloque:
      1. Calcular síndrome (ver HammingCore.decodeBlock)
      2. Si síndrome ≠ 0 → error detectable
      3. Si 1 menor igual a síndrome menor igual a n = error de 1 bit (corregible)
      4. Si síndrome > n → error múltiple (fatal)
      5. Extraer bits de datos del bloque (corregido o no)   */

    public byte[] decode(int[][] noisyBlocks, int originalBits) {
        totalDetected = 0;
        totalCorrected = 0;
        totalFatal = 0;
        blockDetails.clear();

        // Lista temporal para acumular bits de datos
        List<Integer> allDataBitsList = new ArrayList<>();

        for (int bi = 0; bi < noisyBlocks.length; bi++) {
            HammingCore.DecodeResult result = hamming.decodeBlock(noisyBlocks[bi]);

            // Registrar detalle
            blockDetails.add(new BlockDetail(
                bi, result.errorPosition, result.detected, result.corrected
            ));

            // Actualizar estadísticas
            if (result.detected) {
                totalDetected++;
                if (result.corrected) {
                    totalCorrected++;
                } else {
                    totalFatal++;
                }
            }

            // Acumular bits de datos
            for (int bit : result.dataBits) {
                allDataBitsList.add(bit);
            }
        }

        // Convertir List<Integer> a int[]
        int totalDecoded = allDataBitsList.size();
        decodedBits = new int[totalDecoded];
        for (int i = 0; i < totalDecoded; i++) {
            decodedBits[i] = allDataBitsList.get(i);
        }

        // Truncar al tamaño original (eliminar padding de ceros)
        // El padding se añadió en la codificación para completar el último
        // bloque de k bits. Estos bits extra no son datos reales y deben
        // descartarse para que la conversión a bytes sea exacta.
        
        int actualBits = Math.min(totalDecoded, originalBits);
        int[] trimmedBits = new int[actualBits];
        System.arraycopy(decodedBits, 0, trimmedBits, 0, actualBits);

        // Reconstruir bytes desde los bits truncados
        decodedBytes = Transmisor.bitsToBytes(trimmedBits);
        return decodedBytes;
    }

    // Versión sin truncar (compatibilidad, usa todo lo decodificado). 
    public byte[] decode(int[][] noisyBlocks) {
        return decode(noisyBlocks, Integer.MAX_VALUE);
    }
}
