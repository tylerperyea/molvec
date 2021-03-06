package gov.nih.ncats.molvec;

import gov.nih.ncats.common.cli.Cli;
import gov.nih.ncats.common.cli.CliSpecification;
import gov.nih.ncats.common.cli.CliValidationException;
import gov.nih.ncats.molvec.ui.Viewer;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static gov.nih.ncats.common.cli.CliSpecification.*;
/**
 * Created by katzelda on 5/20/19.
 */
public class Main {

    private static class DirectoryProcessor{
        private int numThreads =1;

        private File dir, outputDir;

        public int getNumThreads() {
            return numThreads;
        }

        public void setNumThreads(int numThreads) throws IOException{
            if(numThreads < 1){
                throw new CliValidationException("num of threads must be >=1");
            }
            this.numThreads = numThreads;
        }

        public File getDir() {
            return dir;
        }

        public void setDir(File dir) throws IOException{
            if(!dir.exists()){
                throw new FileNotFoundException("directory '" + dir.getAbsolutePath() + "' does not exist");
            }
            this.dir = dir;
        }

        public File getOutputDir() {
            return outputDir;
        }

        public void setOutputDir(File outputDir) throws IOException {
            if(outputDir !=null){
                Files.createDirectories(outputDir.toPath());
            }
            this.outputDir = outputDir;
        }
    }

    public static void main(String[] args) throws Exception{

        DirectoryProcessor directoryProcessor = new  DirectoryProcessor();

        CliSpecification spec = CliSpecification.createWithHelp(
                option("gui").isFlag(true)
                                    .description("Run Molvec in GUI mode. file and scale option may be set to preload file"),
                radio(
                group(option("f").longName("file")
                        .argName("path")
                        .description("path of image file to process. Supported formats include png, jpeg, tiff.  This option or -dir is required if not using -gui")
                        .setRequired(true),
                        option("o").longName("out")
                                .argName("path")
                                .addValidation(cli->!cli.hasOption("gui"), "-o option not valid with neither -dir nor -gui mode")
                                .description("path of output processed mol. Only valid when not using gui mode. If not specified output is sent to STDOUT")

                        ),
                        group(
                        option("dir")
                                .argName("path")
                                .description("path to a directory of image files to process. Supported formats include png, jpeg, tiff and svg. " +
                                        "Each image file found will be attempted to be processed. If -out or -outDir is not specified then " +
                                        "each processed mol will be put in the same directory and named $filename.mol" +
                                        "This option or -f is required if not using -gui")
                                .setToFile(directoryProcessor::setDir)
                                .setRequired(true),
                                option("outDir")
                                    .argName("path")
                                        .setToFile(directoryProcessor::setOutputDir)
                                    .description("path to output directory to put processed mol files. If this path does not exist it will e created"),
                                option("parallel")
                                        .argName("count")
                                        .setToInt(directoryProcessor::setNumThreads)
                                        .description("Number of images to process simultaneously, if not specified defaults to 1")



                        )),
                 option("scale")
                         .argName("value")
                         .addValidation(cli->cli.hasOption("gui") && cli.hasOption("f"), "scale only valid if specifying file in gui mode")
                         .description("scale of image to show in viewer (only valid if gui mode AND file are specified)")


                )
        .programName("molvec")
        .description("Image to Chemical Structure Extractor Analyzes the given image and tries to find the chemical structure drawn and convert it into a Mol format.")
        .addValidation(cli->cli.hasOption("gui") || cli.hasOption("f") || cli.hasOption("dir"),
                "-f or -dir option is required if not using -gui mode")

        .example("-f /path/to/image.file", "parse the given image file and print out the structure mol to STDOUT")
        .example("-dir /path/to/directory", "serially parse all the image files inside the given directory and write out " +
                        "a new mol file for each image named $image.file.mol the new files will be put in the input directory")
        .example("-dir /path/to/directory -outDir /path/to/outputDir", "serially parse all the image files inside the given directory and write out " +
                        "a new mol file for each image named $image.file.mol the new files will be put in the directory specified by outDir")
        .example("-dir /path/to/directory -parallel 4", "parse in 4 concurrent parallel threads all the image files inside the given directory and write out " +
                        "a new mol file for each image named $image.file.mol the new files will be put in the directory specified by outDir")

        .example("-gui", "open the Molvec Graphical User interface without any image preloaded")
        .example("-gui -f /path/to/image.file", "open the Molvec Graphical User interface  with the given image file preloaded")

        .example("-gui -f /path/to/image.file -scale 2.0", "open the Molvec Graphical User interface  with the given image file preloaded zoomed in/out to the given scale")

                .footer("Developed by NIH/NCATS")
        ;


        if(spec.helpRequested(args)){
            System.out.println(spec.generateUsage());
            return;
        }
        try {
            Cli cli =spec.parse(args);

            if(cli.hasOption("gui")){
                //file and scale
                if(cli.hasOption("f")){

                    String filepath = cli.getOptionValue("f");
                    String scale= "1";
                    if(cli.hasOption("scale")){
                        scale = cli.getOptionValue("scale");
                    }
                    Viewer.main(new String[]{filepath, scale});
                }else {
                    Viewer.main(new String[0]);
                }
            }else if(cli.hasOption("f")){


                String mol = Molvec.ocr(new File(cli.getOptionValue("f")));
                if(cli.hasOption("o")){
                    File outputFile = new File(cli.getOptionValue("o"));
                    File parent = outputFile.getParentFile();
                    if(parent !=null){
                        Files.createDirectories(parent.toPath());
                    }

                    try(PrintWriter writer = new PrintWriter(new FileWriter(outputFile))){
                        writer.println(mol);
                    }
                }else{
                    System.out.println(mol);
                }
            }else if(cli.hasOption("dir")){
                File dir = directoryProcessor.getDir();

                File outputDir = directoryProcessor.getOutputDir();
                if(outputDir ==null){
                    outputDir = dir;
                }
                ExecutorService executorService = Executors.newFixedThreadPool(directoryProcessor.getNumThreads());
                File files[] =dir.listFiles( f->{
                    String name = f.getName();
                    int extOffset = name.lastIndexOf('.');
                    if(extOffset <0){
                        return false;
                    }
                    String ext = name.substring(extOffset+1);
                    return "png".equalsIgnoreCase(ext) || "jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)
                            || "tiff".equalsIgnoreCase(ext);
                });
                if(files ==null || files.length ==0){
                    System.out.println("No image files found");
                    return;
                }
                CountDownLatch latch = new CountDownLatch(files.length);
                for(File f : files){
                    executorService.submit(new MolVecRunnable(f, outputDir, latch));
                }
                executorService.shutdown();
                latch.await();
            }else{
                //invalid
                throw new CliValidationException("gui mode or file not specified");
            }
        }catch(CliValidationException e) {
            System.err.println(e.getMessage());
            System.err.println("\n\n" + spec.generateUsage());
            System.exit(-1);
        }

    }

    private static class MolVecRunnable implements Callable<Void>{
        File f;
        File outDir;
        CountDownLatch latch;
        MolVecRunnable(File f, File outDir, CountDownLatch latch){
            this.f =f;
            this.outDir = outDir;
            this.latch = latch;
        }

        @Override
        public Void call() throws Exception{
            try {
                System.out.println(" .."+f.getName());
                String mol = Molvec.ocr(f);
                File out = new File(outDir, f.getName() + ".mol");
                try (PrintWriter writer = new PrintWriter(out)) {
                    writer.println(mol);
                }
                return null;
            }finally{
                //wait until the end to decrement latch
                latch.countDown();
            }
        }
    }
}
