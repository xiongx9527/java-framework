{
  "properties" : { },
  "id" : "9a8d5688d6a1409f871d3ce81838723d",
  "script" : null,
  "groupId" : "cad348c6243f4419ab0a64dab71e90da",
  "name" : "未定义名称1",
  "createTime" : 1753413871630,
  "updateTime" : 1764509260898,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : null,
  "path" : "test1",
  "description" : null,
  "springBean" : false,
  "enabled" : true
}
================================
package a.b.c;



import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class Test1 {
    private String b = "bbbb99";

    public String test(String a) {
        return a + b + "12312312312abc";
    }

    public void sss(){
        String s = "{}";
    }

    public String abcd() {
        return "ddd==========" ;
    }

}