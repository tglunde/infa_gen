package com.tui.infagen;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import net.sf.saxon.Transform;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Table;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;

@SpringBootApplication
public class Main implements CommandLineRunner {

    @Value("${query.atcom}")
    private String atcomQuery;
    @Value("${query.destimo}")
    private String destimoQuery;
    @Value("${app.subject_area}")
    private String app_subject_area;
    @Value("${app.infa_folder}")
    private String app_infa_folder;
    @Value("${app.infa_project}")
    private String app_infa_project;
    @Value("${app.infa_repo}")
    private String app_infa_repo;
    @Value("${app.infa_dbtype}")
    private String app_infa_dbtype;
    @Value("${app.target_dbtype}")
    private String app_target_dbtype;
    @Value("${app.ts_name}")
    private String app_ts_name;
    @Value("${app.ix_name}")
    private String app_ix_name;
    @Value("${app.infa_target}")
    private String app_infa_target;
    @Value("${app.tmp_dir}")
    private String app_tmp_dir;

    @Value("${atcom.src_dbtype}")
    private String atcom_src_dbtype;
    @Value("${atcom.src_name}")
    private String atcom_src_name;
    @Value("${atcom.src_schema}")
    private String atcom_src_schema;
    @Value("${atcom.infa_source}")
    private String atcom_infa_source;

    @Value("${destimo.src_dbtype}")
    private String destimo_src_dbtype;
    @Value("${destimo.src_name}")
    private String destimo_src_name;
    @Value("${destimo.src_schema}")
    private String destimo_src_schema;
    @Value("${destimo.infa_source}")
    private String destimo_infa_source;

    @Value("#{${bk}}")
    private Map<String,String> bkMap;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        new File(app_tmp_dir + "/elt/").mkdirs();
        new File(app_tmp_dir + "/sqlr/").mkdirs();
        new File(app_tmp_dir + "/sqlha/").mkdirs();
        new File(app_tmp_dir + "/wf/").mkdirs();


        generateArtefactsHA(retrieveMetadata(atcomDataSource(), atcomQuery),
                app_subject_area
                , app_infa_folder, app_infa_project, app_infa_repo, app_infa_dbtype, app_target_dbtype, app_ts_name
                , app_ix_name, atcom_src_dbtype, atcom_src_name, atcom_src_schema
                , app_infa_target, atcom_infa_source
                , app_tmp_dir, "1");
        generateArtefactsExport(retrieveMetadata(destimoDataSource(), destimoQuery),
                app_subject_area
                , app_infa_folder, app_infa_project, app_infa_repo, app_infa_dbtype, app_target_dbtype, app_ts_name
                , app_ix_name, destimo_src_dbtype, destimo_src_name, destimo_src_schema
                , app_infa_target, destimo_infa_source
                , app_tmp_dir, "2");


        //dtd kopieren
        InputStream src = Thread.currentThread().getContextClassLoader().getResourceAsStream("powrmart.dtd");
        Files.copy(src, Paths.get(app_tmp_dir +"/wf/powrmart.dtd"), StandardCopyOption.REPLACE_EXISTING);

        String[] arglist = {"-xsl:wf.xsl" , "-it:main", "-o:workflows.xml"};
        Transform.main(arglist);

    }

    private void generateArtefactsExport(List<Table> tables
            , String subjectArea
            , String infaFolder
            , String infaProject
            , String infaRepository
            , String infaDbType
            , String targetDbType
            , String tablespaceName
            , String indexspaceName
            , String sourceDbType
            , String sourceName
            , String sourceSchema
            , String infaTargetConnection
            , String infaSourceConnection
            , String tempDirectory
            , String interfaceNumber

    ) throws IOException {
        PebbleEngine pEngine = new PebbleEngine.Builder().loader(new ClasspathLoader()).build();
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("infa_folder", infaFolder);
        context.put("infa_project", infaProject);
        context.put("infa_repo", infaRepository);
        context.put("infa_dbtype", infaDbType);
        context.put("table_space_name", tablespaceName);
        context.put("index_space_name", indexspaceName);
        context.put("source_type", sourceDbType);
        context.put("source_name", sourceName);
        context.put("target_type", targetDbType);
        context.put("target_connection", infaTargetConnection);
        context.put("source_connection", infaSourceConnection);
        context.put("subject_area", subjectArea);
        int n = 0;
        for (Table table : tables) {
            context.put("source_table", table.getName());
            context.put("target_schema", subjectArea + "_HA");
            context.put("columns", Arrays.asList(table.getColumns()));
            context.put("bks", getBk(table));
            context.put("table_name", table.getName());
            context.put("schema_name", subjectArea + "_HA");
            context.put("source_schema", sourceSchema);
            context.put("target_schema", subjectArea + "_R");
            context.put("source_connection", infaSourceConnection);
            output(pEngine, "wf_export.XML", context, tempDirectory + "/wf/" + table.getName() + "_export.XML");
        }
    }

    private void generateArtefactsHA(List<Table> tables
            , String subjectArea
            , String infaFolder
            , String infaProject
            , String infaRepository
            , String infaDbType
            , String targetDbType
            , String tablespaceName
            , String indexspaceName
            , String sourceDbType
            , String sourceName
            , String sourceSchema
            , String infaTargetConnection
            , String infaSourceConnection
            , String tempDirectory
            , String interfaceNumber

    ) throws IOException {
        PebbleEngine pEngine = new PebbleEngine.Builder().loader(new ClasspathLoader()).build();
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("infa_folder", infaFolder);
        context.put("infa_project", infaProject);
        context.put("infa_repo", infaRepository);
        context.put("infa_dbtype", infaDbType);
        context.put("table_space_name", tablespaceName);
        context.put("index_space_name", indexspaceName);
        context.put("source_type", sourceDbType);
        context.put("source_name", sourceName);
        context.put("target_type", targetDbType);
        context.put("target_connection", infaTargetConnection);
        context.put("source_connection", infaSourceConnection);
        context.put("subject_area", subjectArea);
        int n = 0;
        for (Table table : tables) {
            context.put("source_table", table.getName());
            context.put("target_schema", subjectArea + "_HA");
            context.put("columns", Arrays.asList(table.getColumns()));
            context.put("bks", getBk(table));
            context.put("table_name", table.getName());
            context.put("schema_name", subjectArea + "_HA");
            context.put("source_schema", sourceSchema);
            output(pEngine, "create_table_r.sql", context, tempDirectory + "/sqlr/V" + interfaceNumber + "_0_" + n + "__" + table.getName() + "_r.sql");
            output(pEngine, "create_table_ha.sql", context, tempDirectory + "/sqlha/V" + interfaceNumber + "_0_" + n++ + "__" + table.getName() + "_ha.sql");
            context.put("target_schema", subjectArea + "_R");

            context.put("source_connection", infaSourceConnection);
            output(pEngine, "wf_replication.XML", context, tempDirectory + "/wf/" + table.getName() + "_R.XML");

            context.put("source_schema", subjectArea + "_R");
            context.put("target_schema", subjectArea + "_HA");
            output(pEngine, "load_ha.sql", context, tempDirectory + "/elt/load_" + table.getName() + "_ha.sql");
            String content = readFile(tempDirectory + "/elt/load_" + table.getName() + "_ha.sql", StandardCharsets.ISO_8859_1);
            context.put("sql_query", content);
            context.put("wf_name", "wf_load_" + sourceName + "_" + table.getName() + "_HA");
            context.put("map_name", "m_load_" + sourceName + "_" + table.getName() + "_HA");
            context.put("ses_name", "s_load_" + sourceName + "_" + table.getName() + "_HA");
            context.put("source_connection", infaTargetConnection);
            output(pEngine, "wf_exec.XML", context, tempDirectory + "/wf/wf_load_" + table.getName() + "_HA.XML");
        }
    }

    private List<Column> getBk(Table table) {
        List<Column> bks = new ArrayList<>();
        for (Column column : table.getColumns()) {
            if (column.isPrimaryKey()) {
                bks.add(column);
            }
        }
        return bks;
    }

    private void output(PebbleEngine pEngine, String templateName, Map<String, Object> context, String targetPath) throws IOException {
        File file = new File(targetPath);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        PebbleTemplate compiledTemplate = pEngine.getTemplate(templateName);
        compiledTemplate.evaluate(writer, context);
        writer.flush();
        writer.close();
    }


    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    private List<Table> retrieveMetadata(DataSource dataSource, String query) throws Exception {
        ResultSet rs = dataSource.getConnection().createStatement().executeQuery(query);

        List<Table> tables = new ArrayList<Table>();
        Table t = new Table();
        while (rs.next()) {
            if (!rs.getString("TABLE_NAME").equals(t.getName())) {
                //neue tabelle
                t = new Table();
                t.setSchema("CUSTOMER_HA");
                t.setName(rs.getString("TABLE_NAME"));
                tables.add(t);
            }
            Column column = new Column();
            column.setName(rs.getString("COLUMN_NAME"));
            if (rs.getString("TYPENAME").equals("int")) {
                column.setType("INTEGER");
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("decimal")) {
                column.setTypeCode(Types.DECIMAL);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("double")) {
                column.setTypeCode(Types.DECIMAL);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("int")) {
                column.setTypeCode(Types.INTEGER);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("integer")) {
                column.setTypeCode(Types.INTEGER);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("bigint")) {
                column.setTypeCode(Types.BIGINT);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("character")) {
                column.setTypeCode(Types.VARCHAR);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("varchar")) {
                column.setTypeCode(Types.VARCHAR);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("timestamp")) {
                column.setTypeCode(Types.TIMESTAMP);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("datetime")) {
                column.setTypeCode(Types.TIMESTAMP);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("date")) {
                column.setTypeCode(Types.DATE);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("smallint")) {
                column.setTypeCode(Types.SMALLINT);
            } else if (rs.getString("TYPENAME").equalsIgnoreCase("bit")) {
                column.setTypeCode(Types.SMALLINT);
            } else {
                throw new RuntimeException("unszpported datatype " + rs.getString("TYPENAME"));
            }

            column.setPrecisionRadix(rs.getInt("NUMERIC_PRECISION"));
            int nScale = rs.getInt("NUMERIC_SCALE");
            if (nScale > 0) {
                column.setScale(rs.getInt("NUMERIC_SCALE"));
            }
            column.setRequired("NO".equals(rs.getString("IS_NULLABLE")));
            String sBk = bkMap.get(t.getName().toUpperCase());
            List<String> bkNames = Arrays.asList( sBk.split(","));
            if (bkNames.contains(column.getName().toUpperCase())) {
                column.setPrimaryKey(true);
                column.setRequired(true);
            }
            t.addColumn(column);
        }
        return tables;

    }
    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.atcom")
    public DataSourceProperties atcomDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.atcom.configuration")
    public BasicDataSource atcomDataSource() {
        return atcomDataSourceProperties().initializeDataSourceBuilder()
                .type(BasicDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties("app.datasource.destimo")
    public DataSourceProperties destimoDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.destimo.configuration")
    public BasicDataSource destimoDataSource() {
        return destimoDataSourceProperties().initializeDataSourceBuilder()
                .type(BasicDataSource.class).build();
    }
}
