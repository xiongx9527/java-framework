const defaultScript = `package demo;

import org.springframework.transaction.annotation.Transactional;

public class UserService {

    @Transactional(rollbackFor = Exception.class)
    public String saveUser() {
        return "ok";
    }
}
`

export default function ($i) {
    return {
        getIcon: item => ['BEAN', '#2da44e'],
        name: $i('springBean.name'),
        language: 'java',
        defaultScript,
        runnable: false,
        requirePath: true,
        merge: item => Object.assign({ enabled: true }, item)
    }
}
