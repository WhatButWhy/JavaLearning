package cn.zyx.test;

import cn.zyx.framework.config.Configuration;
import cn.zyx.framework.config.MappedStatement;
import cn.zyx.po.User;
import org.apache.commons.dbcp.BasicDataSource;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @Description
 * @ClassName MyBatisV2
 * @Author ZhangYixin
 * @date 2020.06.03 20:25
 */
public class MyBatisV2 {

    private Configuration configuration = new Configuration();

    @Test
    public void test(){
        loadXMl("mybatis-conf.xml");

        List<User> users = selectList("getUserById","kobe");
    }

    /**
     * 用来读取XML配置文件
     * @param s
     */
    private void loadXMl(String s) {
        //根据配置文件路径获取对应的输入流
        InputStream inputStream = getResourceAsStream(s);
        //将流对象，转换成document对象
        Document document = creatDocument(inputStream);
        //针对Document对象，按照mybaits的语义去解析document
        //document.getRootElement()获取文档的根节点
        parseConfiguration(document.getRootElement());
    }

    private void parseConfiguration(Element rootElement) {
        //获取environments节点的内容
        Element environments = rootElement.element("environments");
        parseEnvironments(environments);
        //获取mapper节点的内容
        Element mappers = rootElement.element("mappers");
        parseMappers(mappers);
    }

    /**
     * 解析 mappers标签
     * @param mappers
     */
    private void parseMappers(Element mappers) {
        List<Element> mapperList = mappers.elements("mapper");
        for (Element element: mapperList){
            String resource = element.attributeValue("resource");
            //根据配置文件路径获取对应的输入流
            InputStream inputStream = getResourceAsStream(resource);
            //将流对象，转换成document对象
            Document document = creatDocument(inputStream);
            //针对Document对象，按照mybaits的语义去解析document
            //document.getRootElement()获取文档的根节点
            parseMapper(document.getRootElement());
        }
    }

    private void parseMapper(Element rootElement) {
    }

    /**
     * 解析 environments标签
     * @param environments
     */
    private void parseEnvironments(Element environments) {
        //获取标签的default对应的value
        String aDefault = environments.attributeValue("default");
        List<Element> environmentList = environments.elements("environment");
        for (Element element : environmentList){
            String id = element.attributeValue("id");
            //这两个 标签 的对应 属性名 的 值相等再继续处理
            if (id.equals(aDefault)){
                Element dataSource = element.element("dataSource");
                parseDataSource(dataSource);
            }
        }
    }

    /**
     * 解析 DataSource 标签
     * @param dataSource
     */
    private void parseDataSource(Element dataSource) {
        String type = dataSource.attributeValue("type");
        //这里相当于写死 指定数据源为DBCP 其他的数据源类型不进行处理
        if ("DBCP".equals(type)){
            BasicDataSource basicDataSource = new BasicDataSource();
            Properties properties = parseProperty(dataSource);
            //根据name拿到驱动信息传入
            basicDataSource.setDriverClassName(properties.getProperty("driver"));
            //根据name拿到数据库url信息传入
            basicDataSource.setUrl(properties.getProperty("url"));
            //根据name拿到数据库username信息传入
            basicDataSource.setUsername(properties.getProperty("username"));
            //根据name拿到数据库password信息传入
            basicDataSource.setPassword(properties.getProperty("password"));
            configuration.setDataSource(basicDataSource);
        }
    }

    /**
     * 解析 property 标签
     * @param dataSource
     * @return
     */
    private Properties parseProperty(Element dataSource) {
        //创建一个propertis用于返回propertis数据
        Properties properties = new Properties();

        List<Element> elementsList = dataSource.elements("property");
        for (Element element: elementsList){
            String name = element.attributeValue("name");
            String value = element.attributeValue("value");
            properties.put(name,value);
        }
        return properties;
    }

    private Document creatDocument(InputStream inputStream) {
        try {
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(inputStream);
            return document;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private InputStream getResourceAsStream(String s) {
        return this.getClass().getClassLoader().getResourceAsStream(s);
    }

    private <T> List<T> selectList(String statementID,Object param){
        Connection con = null;		//连接
        PreparedStatement preparedStatement = null;	//使用预编译语句
        ResultSet rs = null;	//获取的结果集

        //返回的集合
        List<T> results = new ArrayList<>();
        try {
            MappedStatement mappedStatement = configuration.getMappedStatementById(statementID);

            //获取连接
            Connection connect = getConnection();

            //获取sql
            String sql = getSql();

            //创建statement对象
            preparedStatement = connect.prepareStatement(sql);

            setParameters(preparedStatement,param,mappedStatement);

            //执行sql获取结果集
            rs = preparedStatement.executeQuery();

            handleResultSet(rs,results,mappedStatement);

            // 获取要映射的结果类型
            String resultclassname = properties.getProperty(sqlID + ".resultClassName");
            // 加载指定类并初始化
            Class<?> resultTypeClass = Class.forName(resultclassname);

            Object result = null;
            while (rs.next()){
                //根据指定类创建对应的对象
                result = resultTypeClass.newInstance();

                ResultSetMetaData metaData = rs.getMetaData();
                //得到结果列数
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    //拿到对应列数的列名
                    String columnName = metaData.getColumnName(i);
                    Field field = resultTypeClass.getDeclaredField(columnName);
                    //设置字段的值可以访问
                    field.setAccessible(true);
                    //根据列名设置对应的值
                    field.set(result, rs.getObject(columnName));

                }
                results.add(result);
            }
            return results;

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            // 释放资源
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                }
            }
        }
        return null;
    }

    /**
     * 处理结果集
     * @param rs
     * @param results
     * @param mappedStatement
     * @param <T>
     */
    private <T> void handleResultSet(ResultSet rs, List<T> results, MappedStatement mappedStatement) {

    }

    /**
     * 参数传入处理
     * @param preparedStatement
     * @param param
     * @param mappedStatement
     */
    private void setParameters(PreparedStatement preparedStatement, Object param, MappedStatement mappedStatement) {
        if(SimpleTypeRegistry.isSimpleType(param.getClass())){
            preparedStatement.setObject(1,param);
        }else if (param instanceof Map){

        }else {

        }
    }

    /**
     * 用于获取数据库连接
     * @return
     */
    private String getSql() {
    }

    /**
     * 用于获取数据库连接
     * @return
     */
    private Connection getConnection() {

        try {
            DataSource dataSource = configuration.getDataSource();
            Connection conn = dataSource.getConnection();
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}