export default {
    s3: {
        datasource: {
            title: 'S3 数据源',
            name: 'S3 数据源',
            form: {
                name: '名称',
                key: 'Key',
                platform: '平台',
                accessKey: 'AccessKey',
                secretKey: 'SecretKey',
                endPointSchema: '端点协议',
                endPoint: '端点',
                bucketName: '储桶名称',
                basePath: '基础路径',
                tmpPath: '临时路径',
                expirationSeconds: '有效期(秒)',
                presignedUrlMethod: '预签名方法',
                other: '其它',
                placeholder: {
                    name: '名称，仅显示使用',
                    key: '数据源Key，必填',
                    platform: '请输入平台，必填',
                    accessKey: '请输入 AccessKey，必填',
                    secretKey: '请输入 SecretKey，必填',
                    endPointSchema: '请输入端点协议，例如:https，选填',
                    endPoint: '请输入端点，必填',
                    bucketName: '请输入储桶名称，必填',
                    basePath: '请输入基础路径，必填',
                    tmpPath: '请输入临时路径，选填',
                    expirationSeconds: '请输入有效期(秒)，选填',
                    presignedUrlMethod: '请选择预签名方法，选填',
                    other: '请输入 S3 访问相关配置，如： {xxxx: "yyyy"}'
                }
            }
        }
    }
}