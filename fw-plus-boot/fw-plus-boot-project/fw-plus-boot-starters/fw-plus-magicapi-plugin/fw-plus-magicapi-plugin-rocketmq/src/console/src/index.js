import MagicRocketMqSubscribe from './services/MagicRocketMqSubscribe';
import MagicRocketMqDataSource from './services/MagicRocketMqDataSource';
import MagicRocketMqDataSourceInfo from './components/MagicRocketMqDataSourceInfo.vue';
import MagicRocketMqSubscribeInfo from './components/MagicRocketMqSubscribeInfo.vue';
import zhCN from './locales/zh-cn';
import en from './locales/en';

export default function (opt) {
    const i18n = opt.i18n;
    i18n.add('zh-cn', zhCN);
    i18n.add('en', en);

    return {
        resource: [{
            type: 'rocketmq-subscribe',
            icon: '#magic-rocketmq-rocketmq',
            title: 'rocketmq.subscribe.name',
            service: MagicRocketMqSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
        }],
        datasources: [{
            type: 'rocketmq-datasource',
            icon: '#magic-rocketmq-rocketmq',
            title: i18n.format('rocketmq.datasource.title'),
            name: i18n.format('rocketmq.datasource.name'),
            service: MagicRocketMqDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicRocketMqDataSourceInfo
        }],
        toolbars: [{
            type: 'rocketmq-subscribe',
            title: 'rocketmq.subscribe.title',
            icon: 'parameter',
            component: MagicRocketMqSubscribeInfo
        }]
    };
}
