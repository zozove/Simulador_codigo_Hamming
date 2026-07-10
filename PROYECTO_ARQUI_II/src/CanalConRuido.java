/*
  MÓDULO CANAL CON RUIDO — SIMULACIÓN DE TRANSMISIÓN
  Arquitectura de Computadores — Canal de Comunicación Digital
 
  Simula un Binary Symmetric Channel (BSC) con tres modos de ruido:
    1. BER (Bit Error Rate):    Cada bit tiene probabilidad p de invertirse
    2. Errores exactos totales: Se inyectan EXACTAMENTE N errores (sin repetición)
    3. Errores por bloque:      Se inyectan EXACTAMENTE M errores en CADA bloque
 
  La inyección de ruido se realiza mediante bit flip (XOR con 1):
    0 = 1  o  1 = 0
 
  Se registran todas las posiciones de error para análisis posterior.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CanalConRuido {
    private int totalErrorsInjected; //Contador de errores inyectados
    private final List<int[]> errorPositions;  // Cada entrada: [bloqueIdx, bitIdx]
    private final Random random; //Gennerador de números aleastorios

    public CanalConRuido() { // Constructor del canal con ruido
        this.totalErrorsInjected = 0;
        this.errorPositions = new ArrayList<>(); // Lista para almacenar posiciones de error
        this.random = new Random();
    }

    // Getters
    public int getTotalErrorsInjected() { return totalErrorsInjected; }
    public List<int[]> getErrorPositions() { return errorPositions; }

    /*
      Inyecta ruido probabilístico (Bit Error Rate).
     
      Cada bit del mensaje tiene probabilidad 'ber' de ser invertido.
      Para cada bit se genera un número aleatorio uniforme U[0,1).
      Si U < BER, el bit se invierte mediante XOR con 1.    */

    public int[][] injectBER(int[][] blocks, double ber) {
        int[][] noisy = cloneBlocks(blocks);// copia de los bloques originales para no modificar el original
        totalErrorsInjected = 0;
        errorPositions.clear();

        for (int bi = 0; bi < noisy.length; bi++) { // recorrer cada bloque
            for (int bitIdx = 0; bitIdx < noisy[bi].length; bitIdx++) {
                if (random.nextDouble() < ber) {
                    noisy[bi][bitIdx] ^= 1;  // Bit flip
                    totalErrorsInjected++;
                    errorPositions.add(new int[]{bi, bitIdx});
                }
            }
        }

        return noisy;
    }

    
     // Inyecta exactamente n errores en posiciones aleatorias sin repetición.
     
    public int[][] injectExact(int[][] blocks, int nErrors) {
        int[][] noisy = cloneBlocks(blocks);
        totalErrorsInjected = 0;
        errorPositions.clear();

        // Generar lista de todas las posiciones posibles (bloque, bit)
        List<int[]> allPositions = new ArrayList<>();
        for (int bi = 0; bi < blocks.length; bi++) {
            for (int bitIdx = 0; bitIdx < blocks[bi].length; bitIdx++) {
                allPositions.add(new int[]{bi, bitIdx});
            }
        }

        // Seleccionar n posiciones aleatorias sin repetición
        int nActual = Math.min(nErrors, allPositions.size());
        // Mezclar y tomar los primeros nActual
        java.util.Collections.shuffle(allPositions, random);

        for (int i = 0; i < nActual; i++) {
            int[] pos = allPositions.get(i);
            noisy[pos[0]][pos[1]] ^= 1;
            totalErrorsInjected++;
            errorPositions.add(new int[]{pos[0], pos[1]});
        }

        return noisy;
    }

    /* Inyecta exactamente n errores en CADA bloque.
      Modo ideal para demostraciones académicas:
         0 errores/bloque = canal limpio
         1 error/bloque   = errores corregibles
        2+ errores/bloque = errores fatales (limitación del algoritmo)  */
        
    public int[][] injectPerBlock(int[][] blocks, int nPerBlock) {
        int[][] noisy = new int[blocks.length][];
        totalErrorsInjected = 0;
        errorPositions.clear();

        for (int bi = 0; bi < blocks.length; bi++) {
            noisy[bi] = blocks[bi].clone();
            int n = Math.min(nPerBlock, noisy[bi].length);
            if (n > 0) {
                // Seleccionar n posiciones aleatorias dentro del bloque
                List<Integer> positions = new ArrayList<>();
                for (int i = 0; i < noisy[bi].length; i++) {
                    positions.add(i);
                }
                java.util.Collections.shuffle(positions, random);
                for (int i = 0; i < n; i++) {
                    int bitIdx = positions.get(i);
                    noisy[bi][bitIdx] ^= 1;
                    totalErrorsInjected++;
                    errorPositions.add(new int[]{bi, bitIdx});
                }
            }
        }

        return noisy;
    }

    // Clona una matriz de bloques
    private int[][] cloneBlocks(int[][] blocks) {
        int[][] copy = new int[blocks.length][];
        for (int i = 0; i < blocks.length; i++) {
            copy[i] = blocks[i].clone();
        }
        return copy;
    }
}
