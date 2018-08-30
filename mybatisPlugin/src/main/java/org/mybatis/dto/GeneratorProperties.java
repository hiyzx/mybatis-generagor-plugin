package org.mybatis.dto;

import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author yezhaoxing
 * @since 2018/08/30
 */
@Data
public class GeneratorProperties {

    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private String jdbcClassDriverName;

    private String projectPath;// 项目在硬盘上的基础路径

    private String pluginClass;// 调用mybatis的插件,可自定义如:SpringBootGeneratorPlugin

    private String templatePath = "/src/main/resources/template";// 模板位置

    private String javaPath = "/src/main/java"; // java文件路径

    private String resourcePath = "/src/main/resources";// 资源文件路径

    private String myMapperFile;// 通用mapper的插件类

    private String modelPackage;// po类打包的地址

    private String mapperPackage;// mapper打包的地址

    private String servicePackage;// service打包的地址

    private String abstractService;// 通用service

    private String abstractServiceImpl;// 通用service

    private String author;// 作者

    private String DATE = new SimpleDateFormat("yyyy/MM/dd").format(new Date());// @date

    private List<TableProperties> tablePropertiesList;// 表的属性
}
