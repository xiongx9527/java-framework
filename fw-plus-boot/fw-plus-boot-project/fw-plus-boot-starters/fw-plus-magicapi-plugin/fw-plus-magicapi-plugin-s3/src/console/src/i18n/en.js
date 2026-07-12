export default {
    s3: {
        datasource: {
            title: 'S3 DataSource',
            name: 'S3',
            form: {
                name: 'Name',
                key: 'Key',
                platform: 'Platform',
                accessKey: 'AccessKey',
                secretKey: 'SecretKey',
                endPointSchema: 'EndPoint',
                endPoint: 'EndPoint',
                bucketName: 'Bucket',
                basePath: 'BasePath',
                tmpPath: 'TmpPath',
                expirationSeconds: 'ExpireSec',
                presignedUrlMethod: 'Presigned',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    platform: 'Please Enter Platform, Required',
                    accessKey: 'Please Enter AccessKey, Required',
                    secretKey: 'Please Enter SecretKey, Required',
                    endPointSchema: 'Please Enter EndPoint Schema, e.g: https, Optional',
                    endPoint: 'Please Enter EndPoint, Required',
                    bucketName: 'Please Enter Bucket Name, Required',
                    basePath: 'Please Enter Base Path, Required',
                    tmpPath: 'Please Enter Tmp Path, Optional',
                    expirationSeconds: 'Please Enter Expiration Seconds, Optional',
                    presignedUrlMethod: 'Please Select Presigned Method, Optional',
                    other: 'Please Enter S3 Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}