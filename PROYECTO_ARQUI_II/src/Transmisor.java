/*
  MÓDULO TRANSMISOR — CARGA DE DATOS Y CODIFICACIÓN HAMMING
 
  Arquitectura de Computadores — Canal de Comunicación Digital
 
  Responsabilidades:
  1. Cargar datos desde texto (String) o archivo binario
  2. Convertir bytes a flujo de bits (MSB first)
  3. Codificar los bits usando HammingCore (con padding si necesario)
 
  El orden MSB first (Most Significant Bit first) significa que el primer
  bit del primer byte es el bit de mayor peso (posición 7). Este es el
  estándar en la mayoría de protocolos de comunicación digital.
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Transmisor {
    private final HammingCore hamming;
    private byte[] originalBytes; //Gyarda los datos en forma de bytes
    private String originalPath; //De donde vienen los datos
    private int[][] encodedBlocks; 
    private int totalDataBits;

    public Transmisor(HammingCore hamming) {
        this.hamming = hamming;
        this.originalBytes = new byte[0]; //Areglo vacio 
        this.originalPath = ""; //Texto vacio
        this.encodedBlocks = new int[0][];  //Matriz vacia 
        this.totalDataBits = 0;
    }

    // Getters
    public byte[] getOriginalBytes() { return originalBytes; }
    public String getOriginalPath() { return originalPath; }
    public int[][] getEncodedBlocks() { return encodedBlocks; }
    public int getTotalDataBits() { return totalDataBits; }
    public int getNumBlocks() { return encodedBlocks.length; }
    public int getTotalChannelBits() { return encodedBlocks.length * hamming.getN(); }

    // Carga datos desde una cadena de texto (UTF-8) 
    public void loadText(String text) {
        this.originalBytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        this.originalPath = "<texto>"; //Guarda el origen 
    }

    //Carga datos desde un archivo binario
    public void loadFile(String path) throws IOException {
        this.originalBytes = Files.readAllBytes(Paths.get(path));
        this.originalPath = path;
    }

    
     // Convierte un array de bytes a lista de bits (MSB first).
      
     
    public static int[] bytesToBits(byte[] data) {
        // Cada byte produce 8 bits
        int[] bits = new int[data.length * 8];             //Crea un arreglo de bits. Cada byte tiene 8 bits.
        int idx = 0; // Indice para ir llenanndo el arreglo bits
        for (byte b : data) {
            // Extraer bits de mayor a menor peso (MSB first)
            for (int shift = 7; shift >= 0; shift--) {
                bits[idx++] = (b >> shift) & 1;
                /*
                 b >> shift:  mueve el bit que queremos hasta la última posición.
				& 1 : extrae solo ese bit.
				bits[idx++] : guarda el bit y luego aumenta idx.
                 */
            }
        }
        return bits;
    }

    /*
      Convierte una lista de bits a bytes (MSB first).
      Descarta los bits sobrantes si la longitud no es múltiplo de 8.
      Este es util para la reconstrucción en el receptor.
     */
    public static byte[] bitsToBytes(int[] bits) {
        int usableBits = bits.length - (bits.length % 8); //Calcula cuántos bits se pueden usar formando bytes completos
        byte[] result = new byte[usableBits / 8];
        for (int i = 0; i < usableBits; i += 8) { //Recorre los bits de 8 en 8 
            byte b = 0;
            for (int j = 0; j < 8; j++) {   // Recorre los 8 bits del grupo actual.
                b = (byte) ((b << 1) | bits[i + j]);     //mueve los bits actuales una posición a la izquierda.
            }                                           //agrega el nuevo bit al final.
           
            result[i / 8] = b; //Agrega el bye reconstrudo al final
        }
        return result;
    }

    public void encode() {
        int[] bits = bytesToBits(originalBytes);
        this.totalDataBits = bits.length;
        this.encodedBlocks = hamming.encode(bits);
    }
}