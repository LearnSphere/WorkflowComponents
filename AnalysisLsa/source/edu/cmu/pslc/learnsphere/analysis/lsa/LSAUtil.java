package edu.cmu.pslc.learnsphere.analysis.lsa;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class LSAUtil
{
    public int termCount;
    public int docCount;
    public int dim;
    private Hashtable wordIndex;
    private Hashtable indexWord;
    private float[] entropy;
    private int[] frequency;
    private float[][] vectors;
    private String language = "English";
    ArrayList cPunc = new ArrayList();
    public LSAUtil() throws Exception {
    }

    public String getWordByIndex(Integer index) throws Exception {
        if (!this.indexWord.containsKey(index))
            return "";

        return (String)this.indexWord.get(index);
    }

    public float getEntropyByIndex(int index) throws Exception {
        if (index >= 0 && index < entropy.length)
            return entropy[index];
        else
            return 1;
    }

    public int getFrequency(String word) throws Exception {
        if (!this.wordIndex.containsKey(word))
            return 0;

        int index = (int)this.wordIndex.get(word);
        return this.frequency[index];
    }

    public int getFrequencyByIndex(int index) throws Exception {
        if (index >= 0 && index < this.frequency.length)
            return this.frequency[index];

        return 0;
    }

    public float getVectorLengthByIndex(int index) throws Exception {
        if (index < 0 || index >= this.entropy.length)
            return 0;

        double ret = 0;
        for (int i = 0;i < this.dim;i++)
            ret += this.vectors[index][i] * this.vectors[index][i];
        return (float)Math.sqrt(ret);
    }

    public float getCosine(String text1, String text2) throws Exception {
        float[] v1 = this.getVector(getTokens(text1,this.language));
        float[] v2 = this.getVector(getTokens(text2,this.language));
        return (float)cosine(v1,v2);
    }
    
    public float getEuclidean(String text1, String text2) throws Exception {
        float[] v1 = this.getVector(getTokens(text1,this.language));
        float[] v2 = this.getVector(getTokens(text2,this.language));
        return (float)euclidean(v1,v2);
    }

    public float getChineseCosineByChar(String text1, String text2) throws Exception {
        float[] v1 = this.getVector(getChars(text1));
        float[] v2 = this.getVector(getChars(text2));
        return (float)cosine(v1,v2);
    }

    public float getChineseCosineByToken(String text1, String text2) throws Exception {
        float[] v1 = this.getVector(new String[]{ text1 });
        float[] v2 = this.getVector(new String[]{ text2 });
        return (float)cosine(v1,v2);
    }

    private String[] getChars(String text) throws Exception {
        String[] chars = new String[text.length()];
        for (int i = 0;i < text.length();i++)
            chars[i] = String.valueOf(text.charAt(i));
        return chars;
    }

    private String[] getTokens(String text, String language) throws Exception {
        return this.getEnglishTokens(text);
    }

    public void loadSpace(String path) throws Exception {
        if ((new File(path + "/terms.dat")).exists() && ((new File(path + "/matrix.dat")).exists()))
        {
            this.loadTerms_bin(path);
            this.loadMatrix_bin(path);
        }
        else if ((new File(path + "/voc")).exists() && ((new File(path + "/lsaModel")).exists()))
        {
            this.loadTerms_txt(path);
            this.loadMatrix_txt(path);
        }
        else
        {
            //System.out.println("Space not found!\nPlease specify the right space folder path");
            System.exit(0);
        }
    }

    private void loadMatrix_bin(String path) throws Exception {
        FileInputStream  fs = new FileInputStream (path + "/matrix.dat");
        //ByteBuffer buf = ByteBuffer.allocate(8 * PRIMECOUNT);
        byte[] data = new byte[4];
        fs.read(data, 0, 4);

        docCount = ByteBuffer.wrap(reverseByteOrder(data)).asIntBuffer().get();
        fs.read(data, 0, 4);
        termCount = ByteBuffer.wrap(reverseByteOrder(data)).asIntBuffer().get();
        fs.read(data, 0, 4);
        dim = ByteBuffer.wrap(reverseByteOrder(data)).asIntBuffer().get();
        this.vectors = new float[termCount][dim];
        for (int i = 0;i < termCount;i++)
        {
            for (int j = 0;j < dim;j++){
                fs.read(data, 0, 4);
                this.vectors[i][j] = ByteBuffer.wrap(reverseByteOrder(data)).asFloatBuffer().get();
            }
        }
        fs.close();
    }

    private void loadTerms_bin(String path) throws Exception {
        FileReader  fs2 = new FileReader (path + "/terms.dat");
        BufferedReader sr2 = new BufferedReader(fs2);
        //String[] lines = StringSupport.Trim(TextReaderSupport.readToEnd(sr2)).split(StringSupport.charAltsToRegex("\n".toCharArray()));
        List<String> AllLines = new ArrayList<String>();
        String line = null;
        while ((line = sr2.readLine()) != null) {
            AllLines.add(line);
        }
        String[] lines = new String[AllLines.size()];
        lines=AllLines.toArray(lines);
        sr2.close();
        fs2.close();
        this.entropy = new float[lines.length];
        this.frequency = new int[lines.length];
        this.wordIndex = new Hashtable(lines.length);
        this.indexWord = new Hashtable(lines.length);
        for (int i = 0;i < lines.length;i++)
        {
            String[] st = lines[i].split("\t");
            if (st.length < 3)
                continue;

            String eng = st[0].trim();
            int index = Integer.valueOf(st[1].trim());
            float entropy = Float.parseFloat((st[2].trim()));
            // if (!engIndex.Contains(eng)) continue;
            // string word = (string)engIndex[eng];
            String word = eng;
            this.wordIndex.put(word, index - 1);
            this.indexWord.put(index - 1, word);
            this.entropy[index - 1] = entropy;
            if (st.length > 3)
                this.frequency[index - 1] = Integer.valueOf(st[3].trim());

        }
    }

    private void loadMatrix_txt(String path) throws Exception {
        this.vectors = null;

        int[] fileSize = LineCount(path+ "/lsaModel");
        this.dim = fileSize[1];
        this.vectors = new float[fileSize[0]][dim];

        BufferedReader br = new BufferedReader(new FileReader(new File(path+ "/lsaModel")));
        String line;


        int i = 0;
        while ((line = br.readLine()) != null) {
            String [] temp = line.split(" ");
            for (int j = 0; j < dim; j++) {
                this.vectors[i][j] = Float.parseFloat(temp[j]);
            }
            i++;
        }
    }

    private void loadTerms_txt(String path) throws Exception {
        FileReader fs2 = new FileReader(path + "/voc");
        BufferedReader sr2 = new BufferedReader(fs2);
        List<String> AllLines = new ArrayList<String>();
        String line = null;
        while ((line = sr2.readLine()) != null) {
            AllLines.add(line);
        }
        String[] lines = new String[AllLines.size()];
        lines=AllLines.toArray(lines);
        sr2.close();
        fs2.close();
        this.entropy = new float[lines.length];
        this.frequency = new int[lines.length];
        this.wordIndex = new Hashtable(lines.length);
        this.indexWord = new Hashtable(lines.length);
        for (int i = 0;i < lines.length;i++)
        {
            String word = lines[i].trim();
            this.wordIndex.put(word, i);
            this.indexWord.put(i, word);
            this.entropy[i] = 1;
        }
    }

    private double cosine(float[] v1, float[] v2) throws Exception {
        if (v1 == null || v2 == null || v1.length != v2.length)
            return 0;

        double norm1 = 0;
        double norm2 = 0;
        double prod = 0;
        for (int i = 0;i < v1.length;i++)
        {
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
            prod += v1[i] * v2[i];
        }
        if (norm1 < 0.000001 || norm2 < 0.000001)
            return 0;

        return prod / Math.sqrt(norm1 * norm2);
    }
    
    private double euclidean(float[] a, float[] b) {
        double diff_square_sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            diff_square_sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return Math.sqrt(diff_square_sum);
    }

    private String[] getEnglishTokens(String text) throws Exception {
        String[] tokens = text.trim().toLowerCase().split("[ \t\n\r,.?;:'\"\\[\\]{}=+\\(\\)&%$!0123456789|]");
        return tokens;
    }

    //string[] tokens = text.Trim().Split(" \t\n\r,.?;:'\"[]{}-_=+()*&%$!".ToCharArray());
    private float[] getVector(String[] tokens) throws Exception {
        Hashtable wordFreq = this.getAllWordFrequency(tokens);
        float[] ret = new float[this.dim];
        for (Object __dummyForeachVar0 : wordFreq.keySet())
        {
            String w = (String)__dummyForeachVar0;
            if (!this.wordIndex.containsKey(w))
                continue;

            int index = (Integer)this.wordIndex.get(w);
            double ent = this.entropy[index];
            int f = (Integer)wordFreq.get(w);
            double logf = Math.log(f + 1);
            for (int i = 0;i < this.dim;i++)
                ret[i] = ret[i] + (float)(logf * ent * this.vectors[index][i]);
        }
        return ret;
    }

    private Hashtable getAllWordFrequency(String[] tokens) throws Exception {
        String[] words = tokens;
        Hashtable ret = new Hashtable(words.length);
        for (int i = 0;i < words.length;i++)
        {
            String w = words[i].trim();
            if (w.equals(""))
                continue;

            if (this.cPunc != null && this.cPunc.contains(w))
                continue;

            if (ret.containsKey(w))
            {
                ret.put(w, (Integer)ret.get(w) + 1);
                continue;
            }

            ret.put(w, 1);
        }
        return ret;
    }

    public float getEntropy(String word) throws Exception {
        word = word.trim().toLowerCase();
        if (!this.wordIndex.containsKey(word))
            return 1;

        int index = (Integer)this.wordIndex.get(word);
        return this.entropy[index];
    }

    public static int[] LineCount(String path) throws IOException{
    BufferedReader reader = new BufferedReader(new FileReader(new File(
            path)));
    String line = reader.readLine();
    String [] store = line.split(" ");
    int max_dim = store.length;

    int lineCount = 1;

    while((line = reader.readLine())!= null)
    {
        lineCount++;
        //if (store.length>max_dim){max_dim=store.length;}
    }
    reader.close();
    int [] outcome={lineCount,max_dim};
    return outcome;
    }

    public static byte[] reverseByteOrder(byte[] data){
        byte[] data1 = new byte[4];
        data1[0]=data[3];
        data1[1]=data[2];
        data1[2]=data[1];
        data1[3]=data[0];
        return data1;
    }
}


