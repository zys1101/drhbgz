import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:9000/',
  timeout: 5000
})

// 查询所有空气质量指数级别
export function getAqiList() {
  return request({
    url: '/aqi/list',
    method: 'get'
  })
}

// 根据aqiId查询
export function getAqiById(aqiId) {
  return request({
    url: `/aqi/find/${aqiId}`,
    method: 'get'
  })
}

// 新增
export function saveAqi(data) {
  return request({
    url: '/aqi/save',
    method: 'post',
    data
  })
}

// 修改
export function updateAqi(data) {
  return request({
    url: '/aqi/update',
    method: 'post',
    data
  })
}

// 删除
export function deleteAqi(aqiId) {
  return request({
    url: `/aqi/delete/${aqiId}`,
    method: 'get'
  })
}