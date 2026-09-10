import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:9000/',
  timeout: 5000
})

//请求aqi反馈列表的方法
export function getAqiFeedbackList() {
  return request({
    url: '/aqiFeedback/select',
    method: 'get'
  })
}

//删除aqi反馈的方法
export function deleteByAfid(afId) {
    return request({
        url: `/aqiFeedback/delete/${afId}`,
        method: 'get',
    })
}

//增加aqi反馈的方法
export function saveAqiFeedback(data) {
    return request({
        url: `/aqiFeedback/save`,
        method: 'post', 
        data
    })
}

//根据afId查询aqi反馈的方法
export function getAqiFeedbackById(afId) {
    return request({
        url: `/aqiFeedback/find/${afId}`,
        method: 'get'
    })
}

//修改aqi反馈的方法
export function updateAqiFeedback(data) {
    return request({
        url: `/aqiFeedback/update`,
        method: 'post',
        data
    })
}

//获取所有省份的方法
export function getProvinces(){
    return request({
        url: `/province/list`,
        method: 'get'
    })
}
//根据省id获取所有城市的方法
export function getCityByPid(provinceId){
    return request({
        url: `/province/getCitys?provinceId=${provinceId}`,
        method: 'get'
    })
}