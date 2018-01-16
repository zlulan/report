package com.aventstack.extentreports.reporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.InvalidFileException;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.configuration.Config;
import com.aventstack.extentreports.configuration.ConfigMap;
import com.aventstack.extentreports.model.ScreenCapture;
import com.aventstack.extentreports.model.Test;
import com.aventstack.extentreports.reporter.configuration.ExtentHtmlReporterConfiguration;
import com.aventstack.extentreports.reporter.converters.ExtentHtmlReporterConverter;
import com.aventstack.extentreports.utils.Writer;
import com.aventstack.extentreports.viewdefs.Icon;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

/**
 * The ExtentHtmlReporter creates a rich standalone HTML file. It allows several configuration options
 * via the <code>config()</code> method.
 */
public class ExtentHtmlReporter extends BasicFileReporter implements ReportAppendable {
    
    private static final Logger logger = Logger.getLogger(ExtentHtmlReporter.class.getName());

    private static final String TEMPLATE_LOCATION = "view/html-report";
    private static final String TEMPLATE_NAME = "index.ftl";
    private static final String DEFAULT_CONFIG_FILE = "html-config.properties";

    private static String ENCODING = "UTF-8";
    
    private Boolean appendExisting = false;
    
    private List<Test> parsedTestCollection;
    private ExtentHtmlReporterConfiguration userConfig;
    
    ExtentHtmlReporter() {
        // Required to parse the start and end times in the HTML report.
        Locale.setDefault(Locale.ENGLISH);
        
        loadDefaultConfig();
    }
    
    public ExtentHtmlReporter(String filePath) {
        this();
        this.filePath = filePath;
        config().setFilePath(filePath);
    }
    
    public ExtentHtmlReporter(File file) {
    	this(file.getAbsolutePath());
    }
    
    private void loadDefaultConfig() {
        configContext = new ConfigMap();
        userConfig = new ExtentHtmlReporterConfiguration();
        
        ClassLoader loader = getClass().getClassLoader();
        InputStream is = loader.getResourceAsStream(DEFAULT_CONFIG_FILE);
        loadConfig(is);
    }
    
    public ExtentHtmlReporterConfiguration config() {
        return userConfig;
    }
    
    @Override
    public void start() {
        if (templateMap != null) {
            return;
        }
        
        templateMap = new HashMap<String, Object>();
        templateMap.put("report", this);
        templateMap.put("Icon", new Icon());
        
        BeansWrapperBuilder builder = new BeansWrapperBuilder(Configuration.VERSION_2_3_23);
        BeansWrapper beansWrapper = builder.build();
        
        try {
            TemplateHashModel fieldTypeModel = (TemplateHashModel)beansWrapper.getEnumModels().get(Status.class.getName());
            templateMap.put("Status", fieldTypeModel);
        } 
        catch (TemplateModelException e) {
            logger.log(Level.SEVERE, "", e);
        }
        
        if (appendExisting && filePath != null)
        	parseReportBuildTestCollection();
    }

    private void parseReportBuildTestCollection() {
    	File f = new File(filePath);
    	if (!f.exists())
    		return;
    	
    	ExtentHtmlReporterConverter converter = new ExtentHtmlReporterConverter(filePath);
    	parsedTestCollection = converter.parseAndGetModelCollection();
    }
    
    public void loadRecourseFromJar(String path,String folderPath) throws IOException {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("The path has to be absolute (start with '/').");
        }

        if(path.endsWith("/")){
            throw new IllegalArgumentException("The path has to be absolute (cat not end with '/').");
        }

        int index = path.lastIndexOf('/');

        String filename = path.substring(index + 1);

        // If the folder does not exist yet, it will be created. If the folder
        // exists already, it will be ignored
        File dir = new File(folderPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // If the file does not exist yet, it will be created. If the file
        // exists already, it will be ignored
        if(!folderPath.endsWith(File.separator)){
        	folderPath=folderPath+File.separator;
        }
        filename = folderPath + filename;
        
        File file = new File(filename);

        if (!file.exists() && !file.createNewFile()) {
          
            return;
        }

        // Prepare buffer for data copying
        byte[] buffer = new byte[1024];
        int readBytes;

        // Open and check input stream
        path=path.substring(path.indexOf("jar")+4);
        URL url = getClass().getResource(path);
        URLConnection urlConnection = url.openConnection();
        InputStream is = urlConnection.getInputStream();

        if (is == null) {
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }

        // Open output stream and copy data between source file in JAR and the
        // temporary file
        OutputStream os = new FileOutputStream(file);
        try {
            while ((readBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, readBytes);
            }
        } finally {
            // If read/write fails, close streams safely before throwing an
            // exception
            os.close();
            is.close();
        }

    } 
    
    @Override
    public synchronized void flush() {
        if (testList == null || testList.size() == 0)
            return;
        
        try {
            loadUserConfig();
        } catch (InvalidFileException e) {
            logger.log(Level.SEVERE, "", e);
            return;
        }
        
        if (parsedTestCollection != null && parsedTestCollection.size() > 0)
        	for (int ix = 0; ix < parsedTestCollection.size(); ix++)
        		testList.add(ix, parsedTestCollection.get(ix));
        
        parsedTestCollection = null;
        
        String extentSource = null;
        
        try {
            Template template = getConfig().getTemplate(TEMPLATE_NAME);
            
            StringWriter out = new StringWriter();
            
            template.process(templateMap, out);
            extentSource = out.toString();
            
            out.close();
        }
        catch (IOException | TemplateException e) {
            logger.log(Level.SEVERE, "Template not found", e);
        }
        File f=new File(filePath);
        String path=f.getParentFile().getAbsolutePath();
        if(!path.endsWith(File.separator)){
        	path=path+File.separator;
        }
        File dir=new File(path+"extent");
    	if(!dir.exists()||!dir.isDirectory()){
    		dir.mkdirs();
    		 URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
    		 try {
				String oldpath = java.net.URLDecoder.decode(url.getPath(), "utf-8");
		        String css=oldpath+"/css.css";
	            String extent=oldpath+"/extent.css";
	            String js=oldpath+"/extent.js";
	            String icon=oldpath+"/icon.css";
				loadRecourseFromJar(css,dir.getAbsolutePath());
				loadRecourseFromJar(extent,dir.getAbsolutePath());
			    loadRecourseFromJar(js,dir.getAbsolutePath());
			    loadRecourseFromJar(icon,dir.getAbsolutePath());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
         
    	}
        Writer.getInstance().write(f, extentSource);        
    }
    
    private void loadUserConfig() throws InvalidFileException {
        String filePath = userConfig.getConfigMap().get("filePath");
        
        if (filePath == null && this.filePath == null)
            throw new InvalidFileException("No file specified.");
        
        userConfig.setFilePath(this.filePath);
        
        userConfig.getConfigMap().forEach(
            (k, v) -> {
                if (v != null) {
                    Config c = new Config();
                    c.setKey(k);
                    c.setValue(v);
                    
                    configContext.setConfig(c); 
                }
            }
        );
    }
    
    private Configuration getConfig() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);

        cfg.setClassForTemplateLoading(ExtentReports.class, TEMPLATE_LOCATION);
        cfg.setDefaultEncoding(ENCODING);
        
        return cfg;
    }
    
    @Override
    public void onScreenCaptureAdded(Test test, ScreenCapture screenCapture) throws IOException { }

    @Override
    public void setTestList(List<Test> reportTestList) {
        testList = reportTestList;
    }
    
    public List<Test> getTestList() {
        if (testList == null)
            testList = new ArrayList<>();
        
        return testList;
    }
    
    public boolean containsStatus(Status status) {
        boolean b = statusCollection == null || statusCollection.isEmpty() ? false : statusCollection.contains(status);
        return b;
    }
    
    public ConfigMap getConfigContext() { 
        return configContext; 
    }

	@Override
	public void setAppendExisting(Boolean b) {
		this.appendExisting = b;
	}
    
}
