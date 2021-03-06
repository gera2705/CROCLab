import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Handler {

    private static String[] inputFilePaths;
    private static String outputFilePath;
    private static String quarantineFilePath;
    private static String statisticsFilePath;

    private static ArrayList<String>quarantineSentences = new ArrayList<>();


    public static void main(String[] args) throws FileNotFoundException {
        String inputFilePath = "C:/Users/ivan/Desktop/CROCLab/Files/InputFile.txt";
        String outputFilePath = "C:/Users/ivan/Desktop/CROCLab/Files/OutputFile.txt";
        String quarantineFilePath = "C:/Users/ivan/Desktop/CROCLab/Files/QuarantineFile.txt";
        String[] myArgs = new String[7];
        myArgs[0] = "3";
        myArgs[1] = "C:/Users/ivan/Desktop/CROCLab/Files/InputFile1.txt";
        myArgs[2] = "C:/Users/ivan/Desktop/CROCLab/Files/InputFile2.txt";
        myArgs[3] = "C:/Users/ivan/Desktop/CROCLab/Files/InputFile3.txt";
        myArgs[4] = "C:/Users/ivan/Desktop/CROCLab/Files/OutputFile.txt";
        myArgs[5] = "C:/Users/ivan/Desktop/CROCLab/Files/QuarantineFile.txt";
        myArgs[6] = "C:/Users/ivan/Desktop/CROCLab/Files/StatisticsFile.txt";
        initializeFilePaths(myArgs);

        String dataInputFile = readInputFiles(inputFilePaths);

        ArrayList<String>crudeSentences = getSentences(dataInputFile);

        ArrayList<String>clearedSentences = removeQuarantineSentence(crudeSentences);

        createOutputFile(clearedSentences, outputFilePath);
        createQuarantineFile(quarantineSentences, quarantineFilePath);
        createStatisticsFile(statisticsFilePath, crudeSentences, clearedSentences, quarantineSentences);

        System.out.println();

    }

    private static void initializeFilePaths(String args[]){
        //первый агрумент ком. строки - кол-во входных файлов с сырым текстом.
        int countInputFile = Integer.parseInt(args[0]);
        inputFilePaths = new String[countInputFile];
        for (int i = 0; i < countInputFile; i++){
            inputFilePaths[i] = args[i+1];
        }
        outputFilePath = args[countInputFile+1];
        quarantineFilePath = args[countInputFile+2];
        statisticsFilePath = args[countInputFile+3];
    }

    //Принимает массив из путей к файлам. Возвращает строку, в которую записано содержимое всех файлов.
    private static String readInputFiles(String ... filePaths){
        String dataInputFile = "";
        for (String filePath : filePaths) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)))) {
                String readingString = "";
                while ((readingString = br.readLine()) != null) {
                    dataInputFile += readingString;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return  dataInputFile;
    }


    //Разбивает строку с файлом на предложения. Каждая строка в листе - отдельное предложение.
    private static ArrayList<String>getSentences(String data){
        ArrayList<String>sentences = new ArrayList<>();
        Pattern pattern = Pattern.compile("[A-Я].*?[\\.\\?\\!]");
        Matcher matcher = pattern.matcher(data);
        while(matcher.find()){
            sentences.add(data.substring(matcher.start(), matcher.end()));
        }
        return sentences;
    }


    private static ArrayList<String>removeQuarantineSentence(ArrayList<String>crudeSentences){
        ArrayList<String>clearedSentence = new ArrayList<>(crudeSentences);
        Iterator<String> iterator = clearedSentence.iterator();
        while(iterator.hasNext()){
            String sentence = iterator.next();
            Pattern pattern = Pattern.compile("[\\d]");
            Matcher matcher = pattern.matcher(sentence);
            if (matcher.find()){
                quarantineSentences.add(sentence);
                iterator.remove();
            }
        }
        return clearedSentence;
    }


    private static void createStatisticsFile(String statisticsFilePath, ArrayList<String>crudeSentences,
                                              ArrayList<String>clearedSentences, ArrayList<String>quarantineSentences) {
    int countSentence = crudeSentences.size();
    Map<String, Integer> countIdenticalSentence = getCountIdenticalSentence(crudeSentences);
    Map<String, Integer>countEachWord = getCountEachWord(crudeSentences);
    //суммируем кол-во вхождений каждого слова в текст(кол-во слов в тексте).
    int countAllWords = countEachWord.entrySet().stream().mapToInt(word -> word.getValue()).sum();
    int countReplacement;
    int countQuarantineSentence = quarantineSentences.size();
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(statisticsFilePath, Charset.forName("UTF-8")));) {
            bw.write("Общее количество предложений: " + countSentence + "\n");

            bw.write("Количество одинаковых предложений:\n" );
            if (countIdenticalSentence.size() == 0){
                bw.write("0\n");
            }else {
                for (Map.Entry pair : countIdenticalSentence.entrySet()) {
                    bw.write("\t" + pair.getKey() + "-" + pair.getValue() + " раз.\n");
                }
            }
            bw.write("Количество одинаковых слов:\n");
            for (Map.Entry pair : countEachWord.entrySet()){
                bw.write("\t" + pair.getKey() + "-" + pair.getValue() + "раз\n");
            }
            bw.write("Количество слов в тексте: " + countAllWords + "\n");
            bw.write("Количество фраз помещённых в карантин: " + countQuarantineSentence);
        }catch(IOException e){
            e.printStackTrace();
        }
    }


    // Принимает все неочищенные предложения. Возвращает map с кол-вом повторов предложения в тексте
    // (для каждого предложения). Если предложение встречается 1 раз, то оно не содержится в выходном map.
    private static Map<String, Integer> getCountIdenticalSentence(ArrayList<String>crudeSentences){
        Map<String, Integer>sentenceCount = new HashMap<>();
        for (String sentence : crudeSentences){
            if (!sentenceCount.containsKey(sentence)){
                sentenceCount.put(sentence, 1);
            }else{
                sentenceCount.replace(sentence, sentenceCount.get(sentence)+1);
            }
        }
        sentenceCount.entrySet().removeIf(sentence -> (sentence.getValue() == 1));
        return sentenceCount;
    }

    //Принимает список неочищенных предложений. Возвращает кол-во повторов слова в тексте(для каждого слова).
    private static Map<String, Integer>getCountEachWord(ArrayList<String>crudeSentences){
        Map<String, Integer>wordCount = new TreeMap<>();
        for (String sentence : crudeSentences){
            String[]words = sentence.split(" ");
            for (String word : words){
                if (!wordCount.containsKey(word)){
                    wordCount.put(word, 1);
                }else{
                    wordCount.replace(word, wordCount.get(word)+1);
                }
            }
        }
        return wordCount;
    }


    private static void createOutputFile(ArrayList<String>cleanSentences, String outputFilePath){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath, Charset.forName("UTF-8")));){
            for (String sentence : cleanSentences){
                bw.write(sentence + "\n");
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
    }


    private static void createQuarantineFile(ArrayList<String>quarantineSentences, String quarantineFilePath){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(quarantineFilePath, Charset.forName("UTF-8")));){
            for (String sentence : quarantineSentences){
                bw.write(sentence + "\n");
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }


}
