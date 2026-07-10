/*
  NÚCLEO DEL ALGORITMO HAMMING PARAMETRIZABLE

  Arquitectura de Computadores - Canal de Comunicación Digital
 
  Implementa codificación y decodificación Hamming con detección y
  corrección de errores de 1 bit. Soporta cualquier configuración
  donde n = 2^r - 1, k = n - r (Hamming(7,4), Hamming(15,11), etc.)

 
 SÍNDROME:
  El síndrome se calcula recalculando los bits de paridad a partir de los
  datos recibidos. Si el síndrome es un número entre 1 y n, indica la
 posición exacta del bit erróneo. Si es 0, no hay error detectable.
  Si está fuera del rango [1,n], hay error múltiple no corregible.
 
  Cada bit de paridad en posición 2^i cubre todas las posiciones cuyo
  índice binario tiene el bit i-ésimo activo. Por ejemplo en Hamming(7,4):
     P1 (pos 1) cubre: 1, 3, 5, 7
     P2 (pos 2) cubre: 2, 3, 6, 7
   P4 (pos 4) cubre: 4, 5, 6, 7
  Esto garantiza que el síndrome (concatenando los bits de paridad
  recalculados) da la posición binaria del error.
 */

public class HammingCore {
    private final int r;             // Bits de paridad
    private final int k;             // Bits de datos por bloque
    private final int n;             // Bits totales por bloque
    private final int[] parityPositions;  // Potencias de 2 (1,2,4,8, etc)
    private final int[] dataPositions;    // Posiciones no-potencia-de-2

    /*
      Inicializa el código Hamming.
         El número de paridad (r) se calcula como el mínimo que cumple:
        2^r -1 >= k + r
      Esto asegura que el síndrome de r bits pueda representar los
      n+1 estados posibles (n posiciones de error + "sin error").
     */
    public HammingCore(int dataBits) {
        this.k = dataBits;

        // Calcular r (bits de paridad)
        int rCalc = 1;
        while ((1 << rCalc) < (dataBits + rCalc + 1)) {
            rCalc++;
        }
        this.r = rCalc;
        this.n = (1 << r) - 1;  // n = 2^r - 1

        // Posiciones de paridad: potencias de 2 (1-indexed)
        this.parityPositions = new int[r];
        for (int i = 0; i < r; i++) {
            parityPositions[i] = 1 << i;  // 1, 2, 4, 8, 2^n
        }

        // Posiciones de datos: complemento a las de paridad
        // Se cuentan para saber cuántas hay
        int dataCount = 0;
        for (int p = 1; p <= n; p++) {
            boolean isParity = false;
            for (int pp : parityPositions) {
                if (p == pp) { isParity = true; break; }
            }
            if (!isParity) dataCount++;
        }
        this.dataPositions = new int[dataCount];
        int idx = 0;
        for (int p = 1; p <= n; p++) {
            boolean isParity = false;
            for (int pp : parityPositions) {
                if (p == pp) { isParity = true; break; }
            }
            if (!isParity) {
                dataPositions[idx++] = p;
            }
        }
    }

    // Getters
    public int getR() { return r; }
    public int getK() { return k; }
    public int getN() { return n; }
    public double getEfficiency() { return (double) k / n; }

    @Override
    public String toString() {
        return String.format("Hamming(%d,%d) [r=%d, n=%.2f%%]", n, k, r, getEfficiency() * 100);
    }


    // CODIFICACIÓN

    public int[] encodeBlock(int[] dataBits) {
        if (dataBits.length != k) {
            throw new IllegalArgumentException(
                "Se requieren " + k + " bits de datos, se recibieron " + dataBits.length);
        }

        // Inicializar bloque de n bits
        int[] block = new int[n];

        // Colocar bits de datos en posiciones no-paridad
        for (int i = 0; i < dataPositions.length && i < dataBits.length; i++) {
            int pos = dataPositions[i];       // Posición 1-indexed
            block[pos - 1] = dataBits[i];     // Convertir a 0-indexed
        }

        // Calcular bits de paridad
        for (int parPos : parityPositions) {
            int xorAcc = 0;
            // XOR de todas las posiciones cuyo bit parPos está activo
            for (int pos = parPos; pos <= n; pos++) {
                if ((pos & parPos) != 0) {
                    xorAcc ^= block[pos - 1];
                }
            }
            block[parPos - 1] = xorAcc;
        }

        return block;
    }

    /*
      Codifica una secuencia larga de bits en múltiples bloques Hamming.
      Aplica padding con ceros al último bloque si es necesario.
     */
    public int[][] encode(int[] dataBits) {
        // Calcular padding necesario
        int remainder = dataBits.length % k;
        int paddedLen = dataBits.length;
        if (remainder != 0) {
            paddedLen = dataBits.length + (k - remainder);
        }

        // Fragmentar y codificar
        int numBlocks = paddedLen / k;
        int[][] blocks = new int[numBlocks][n];

        for (int b = 0; b < numBlocks; b++) {
            int[] chunk = new int[k];
            for (int j = 0; j < k; j++) {
                int srcIdx = b * k + j;
                if (srcIdx < dataBits.length) {
                    chunk[j] = dataBits[srcIdx];
                } else {
                    chunk[j] = 0;  // Padding con cero
                }
            }
            blocks[b] = encodeBlock(chunk);
        }

        return blocks;
    }

    // DECODIFICACIÓN


    /*
      Resultado de la decodificación de un bloque Hamming.
     Contiene los datos extraídos y las banderas de detección/corrección.
     */
    public static class DecodeResult {
        public final int[] dataBits;     // k bits de datos (corregidos si fue posible)
        public final int errorPosition;  // 0=sin error, 1..n=posición, -1=error múltiple
        public final boolean detected;   // true si se detectó error
        public final boolean corrected;  // true si se corrigió (error 1-bit)

        public DecodeResult(int[] dataBits, int errorPosition, boolean detected, boolean corrected) {
            this.dataBits = dataBits;
            this.errorPosition = errorPosition;
            this.detected = detected;
            this.corrected = corrected;
        }
    }

    /*
      Decodifica un bloque Hamming recibido, detecta y corrige errores.
     
      ALGORITMO DEL SÍNDROME
      */
    public DecodeResult decodeBlock(int[] received) {
        if (received.length != n) {
            throw new IllegalArgumentException(
                "Se requieren " + n + " bits, se recibieron " + received.length);
        }

        // CÁLCULO DEL SÍNDROME
        int syndrome = 0;
        for (int parPos : parityPositions) {
            int xorCheck = 0;

            // Recalcular paridad para esta posición
            for (int pos = 1; pos <= n; pos++) {
                if ((pos & parPos) != 0) {
                    xorCheck ^= received[pos - 1];
                }
            }
            // Si la paridad recalculada es 1, hay discrepancia
            if (xorCheck != 0) {
                syndrome += parPos;
            }
        }

        //ANÁLISIS DEL SÍNDROME
        if (syndrome == 0) {
            // Caso 1: Sin errores detectables
            int[] data = extractDataBits(received);
            return new DecodeResult(data, 0, false, false);
        }

        if (syndrome >= 1 && syndrome <= n) {
            // Caso 2: Error de 1 bit - CORREGIBLE
            int[] corrected = received.clone();
            corrected[syndrome - 1] ^= 1;  // Invertir bit erróneo
            int[] data = extractDataBits(corrected);
            return new DecodeResult(data, syndrome, true, true);
        }

        // Caso 3: Error múltiple, no corregible
        int[] data = extractDataBits(received);
        return new DecodeResult(data, -1, true, false);
    }

    /*
      Extrae los bits de datos de un bloque Hamming decodificado.     */
    private int[] extractDataBits(int[] block) {
        int[] data = new int[k];
        for (int i = 0; i < k && i < dataPositions.length; i++) {
            data[i] = block[dataPositions[i] - 1];
        }
        return data;
    }
}
