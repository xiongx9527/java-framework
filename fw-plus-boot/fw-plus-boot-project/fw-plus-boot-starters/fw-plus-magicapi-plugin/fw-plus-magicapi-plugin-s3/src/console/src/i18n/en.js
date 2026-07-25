export default {
    s3: {
        browser: {
            title: 'S3 File Manager',
            selectStorage: 'Select storage',
            path: 'Path',
            basePath: 'Base path',
            save: 'Save',
            search: 'Search files',
            searchAction: 'Search',
            loading: 'Loading...',
            empty: 'No files found',
            loadMore: 'Load more',
            previewUrl: 'Preview URL',
            preview: 'Preview',
            previewFailed: 'Unable to generate preview URL',
            download: 'Download',
            downloadFailed: 'Unable to generate download URL',
            delete: 'Delete',
            confirmDelete: 'Delete {0}?',
            cancel: 'Cancel',
            loadFailed: 'Unable to load S3 files'
        },
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
