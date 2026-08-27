const defaultScript = `package demo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("sys_user")
@Table(value = "sys_user")
public class User {

    @TableId("id")
    @com.mybatisflex.annotation.Id
    private Long id;

    @TableField("user_name")
    @Column("user_name")
    @com.alibaba.fastjson.annotation.JSONField(name = "user_name")
    @com.alibaba.fastjson2.annotation.JSONField(name = "user_name")
    @JsonProperty("user_name")
    private String userName;

    @TableField("password")
    @Column("password")
    @com.alibaba.fastjson.annotation.JSONField(serialize = false, deserialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false, deserialize = false)
    @JsonIgnore
    private String password;
}
`

export default function ($i) {
    return {
        getIcon: item => ['JAVA', '#d73a49'],
        name: $i('javaEntity.name'),
        language: 'java',
        defaultScript,
        runnable: false,
        requirePath: true,
        merge: item => Object.assign({ enabled: true }, item)
    }
}
