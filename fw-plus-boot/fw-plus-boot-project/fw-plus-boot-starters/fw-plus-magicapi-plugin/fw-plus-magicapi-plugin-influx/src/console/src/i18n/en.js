export default {
    influx: {
        datasource: {
            title: 'Influx DataSource',
            name: 'Influx',
            form: {
                name: 'Name',
                key: 'Key',
                url: 'Url',
                username: 'Username',
                password: 'Password',
                token: 'Token',
                org: 'Org',
                bucket: 'Bucket',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    url: 'Please Enter Url, Required',
                    username: 'Please Enter username, Optional',
                    password: 'Please Enter password, Optional',
                    token: 'Please Enter token, Optional',
                    org: 'Please Enter org, Optional',
                    bucket: 'Please Enter bucket, Optional',
                    other: 'Please Enter Influx Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}