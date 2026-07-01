<template>
    <div class="magic-login">
        <div class="magic-login-box">
            <div class="magic-login-logo"></div>
            <div class="magic-login-text">Magic-API <span v-if="constants.MAGIC_API_VERSION_TEXT">v{{constants.MAGIC_API_VERSION_TEXT}}</span></div>
            <div class="magic-login-row error" v-if="error"><magic-icon icon="error"/><span>{{ error }}</span></div>
            <div class="magic-login-row">
                <magic-icon icon="user"/>
                <magic-input :onEnter="doLogin" v-model:value="username" :placeholder="$i('message.username')"/>
            </div>
            <div class="magic-login-row">
                <magic-icon icon="password"/>
                <magic-input :onEnter="doLogin"  v-model:value="password" type="password" :placeholder="$i('message.password')"/>
            </div>
            <div class="magic-login-row">
                <magic-button :value="$i('message.login')" @onClick="doLogin"/>
            </div>
        </div>
        <div class="magic-login-copyright">Copyright © 2020-{{new Date().getYear() + 1900}} <a href="https://ssssssss.org.cn" target="_blank">ssssssss.org.cn</a> All rights reserved.</div>
    </div>
</template>
<script setup>
import { ref } from 'vue'
import bus from '../../../scripts/bus.js'
import $i from '../../../scripts/i18n.js'
import constants from '../../../scripts/constants.js'
import Message from '../../../scripts/constants/message.js'
import request from '../../../scripts/request.js'
import store from '../../../scripts/store.js'
defineProps({
    value: Boolean,
    error: String
})
const username = ref('')
const password = ref('')
const emit = defineEmits(['update:value', 'update:error'])
const doLogin = () => {
    if(username.value && password.value){
        emit('update:error', null)
        request.sendPost('/login', { username: username.value, password: password.value }).success((successed, response) => {
            if(successed){
                emit('update:value', false)
                constants.HEADER_MAGIC_TOKEN_VALUE = response.headers[constants.HEADER_MAGIC_TOKEN]
                store.set(constants.STORE.token, constants.HEADER_MAGIC_TOKEN_VALUE)
                bus.$emit(Message.LOGINED)
                username.value = ''
                password.value = ''
            }
        }).exception((code, message) => {
            if(code != 401){
                message = translateCode(code, message)
            }
            emit('update:error', message)
        }).error((_code, _message, error) => {
            if(error){
                let message = ''
                if (error.response) {
                    message = JSON.stringify(error.response.data || '') || $i('code.invalid', error.response.status)
                } else {
                    message = error.message
                }
                emit('update:error', message)
            }
        })
    }
}
</script>
<style scoped>
label {
  width: 60px;
  text-align: right;
  display: inline-block;
}
.magic-login{
    position: fixed;
    z-index: 999999999999;
    background: #fff;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-image: url(../../../assets/login-bg.svg);
}
.magic-login .magic-login-box{
    border-radius: 5px;
    width: 450px;
    box-shadow: 0 7px 25px rgba(0,0,0,.08);
    position: absolute;
    box-sizing: border-box;
    padding-top:60px;
    top:33.333333%;
    margin-top: -125px;
    background-color: #fff;
}
.magic-login-logo{
    background-image: url(../../../assets/logo-magic-api.png);
    width: 128px;
    height: 128px;
    background-size: 90px 90px;
    background-repeat: no-repeat;
    background-position: center center;
    position: absolute;
    top: -64px;
    left: 50%;
    margin-left: -64px;
    border-radius: 64px;
    box-shadow: 0 0 20px 5px rgba(0,0,0,.08);
    padding: 10px;
    background-color: #fff;
}
.magic-login-text{
    height: 70px;
    line-height: 70px;
    display: block;
    text-align: center;
    font-family: PoetsenOne;
    font-size: 28px;
    color: #808080;
}
.magic-login-text span{
    font-size: 16px;
}
.magic-login-copyright{
    text-align: center;
    color: #999;
    font-size: 18px;
    font-family: Avenir,Helvetica,Arial,sans-serif;
    position: absolute;
    bottom: 50px;
}
.magic-login-copyright a{
    text-decoration: none;
    color: #2196f3;
    outline: 0;
}
.magic-login-row{
    width: 400px;
    margin:25px auto;
    position: relative;
}
.magic-login-row.error{
    background-color: #fff1f0;
    border: 1px solid #ffa39e;
    color: #000;
    border-radius: 4px;
    padding:10px 0;
    padding-left: 40px;
}
.magic-login-row.error span{
    font-size: 14px;
    word-break: break-all;
}
.magic-login-row svg{
    position: absolute;
    width: 20px;
    height: 20px;
    top: 15px;
    left: 15px;
}
.magic-login-row.error svg{
    top: 10px;
}
.magic-login-row :deep(.magic-icon-error){
    fill: red;
}
.magic-login-box .magic-input{
    height: 50px;
    line-height: 50px;
    background-color: transparent;
    display: block;
    border: 1px solid #D9D9D9;
    border-radius: 4px;
    padding-left: 40px;
    color: rgba(0,0,0,.65);
    transition: all .3s;
    font-size: 16px;
}
.magic-login-box .magic-input:focus{
    border-color:#0784de;
}
.magic-login-box .magic-button{
    width: 100%;
    height: 50px;
    line-height: 50px;
    background-color: #2196f3;
    color: #fff;
    border-radius: 4px;
    font-size: 18px;
    border-color: #2196f3;
    transition: all .3s;
}
.magic-login-box .magic-button:hover,
.magic-login-box .magic-button:focus{
    background-color: #4db5ff !important;
    border-color: #4db5ff !important;
    color: #fff !important;
}
.magic-login-box .magic-button:active{
    background-color: #1272cc;
    border-color: #1272cc;
}
</style>
