
// AJAX
var http =
{
    createRequest: function createHttpRequest()  {
        try  {  return new XMLHttpRequest();  }
        catch (e)  {
            try  {  return new ActiveXObject("Microsoft.XMLHTTP");  }
            catch (e)  {  return new ActiveXObject("Msxml2.XMLHTTP");  }
        }
    },

    status: undefined,

    // выполняет http-запрос
    // если указан handler, то асинхронный
    execute: function (method, url, content, contentType, handler, errorHandler)
    {
        this.status = undefined;
        //    создать объект запроса
        var httpRequest = this.createRequest();
        //    открыть
        httpRequest.open(method, url, handler!==undefined);
        //    установить обработчики
        if (handler!==undefined)  httpRequest.onreadystatechange =  function()  {
                if (httpRequest.readyState == 4 && httpRequest.status != 0)  {
                    handler(httpRequest.responseText, httpRequest.status);
                }
            };
        if (errorHandler)  httpRequest.onerror = errorHandler;
        //    установить Content-Type (если не указан, установит браузер)
        if (contentType)  httpRequest.setRequestHeader("Content-Type", contentType);
        //    отправить запрос
        httpRequest.send(content===undefined ? null : content);
        //    если установлен обработчик - больше ничего делать не надо
        if (handler!==undefined)  return httpRequest;
        //    если нет - сохранить статус и вернуть текст ответа
        this.status = httpRequest.status;
        return httpRequest.responseText;
    },

    // далее функции-обертки

    get: function (url, resultHandler, errorHandler)
    {
        return this.execute("GET", url, undefined, undefined, resultHandler, errorHandler);
    },

    post: function (url, content, contentType, resultHandler, errorHandler)
    {
        return this.execute("POST", url, content, contentType, resultHandler, errorHandler);
    },

    urlEncoded: function (params)
    {
        var res = [];
        for (var p in params) if (params.hasOwnProperty(p))  {
            var par = params[p];
            if (par instanceof Array)  {
                for (var i=0; i<par.length; i++)  {
                    if (res.length)  res.push("&");
                    res.push(p, '=', encodeURIComponent(par[i]));
                }
            }
            else  {
                if (res.length)  res.push("&");
                res.push(p, '=', encodeURIComponent(par));
            }
        }
        return res.join('');
    }

    //multipartBoundary: function()
    //{
    //    return String(Math.random()).slice(2);
    //},
    //
    //multipart: function (data, boundary)
    //{
    //    var boundaryMiddle = '--' + boundary + '\r\n';
    //    var boundaryLast = '--' + boundary + '--\r\n';
    //
    //    var body = ['\r\n'];
    //    for (var key in data)  {
    //        if (body.length)  body.push(boundaryMiddle);
    //        body.push('Content-Disposition: form-data; name="', key, '"\r\n\r\n', data[key], '\r\n');
    //    }
    //    body.push(boundaryLast);
    //
    //    return body.join();
    //}
};


function error(e, logError)
{
    var element = document.getElementById("error");
    if (element)  {  element.style.display = "block";  element.innerText = e.toString();  }
    else  alert(e.toString());
    console.error(logError ? logError : e);
}

function success(e)
{
    error(e);
}

function hideError()
{
    var element = document.getElementById("error");
    if (element)  element.style.display = "none";
}

function reloadPage()
{
    window.location.reload(true);
}

function action(url, params, onSuccess, onError)
{
    if (!onSuccess)  onSuccess = function()  {  success("cохранено");  };
    else if (typeof onSuccess === "string")  {  var onSuccessMessage=onSuccess;  onSuccess = function()  {  success(onSuccessMessage);  };  }

    hideError();
    try
    {
        var content =  params instanceof FormData ? params : http.urlEncoded(params);
        var contentType = params instanceof FormData ? undefined : "application/x-www-form-urlencoded; charset=utf-8";
        http.post("${root}/"+url, content, contentType,
        function (resultText)  {
            try  {
                var res = JSON.parse(resultText);
                if (res.error)  {  error(res.error);  if (onError)  onError(e);  }
                else if (res.result!==undefined)  onSuccess(res.result);
                else  error("Ошибка взаимодействия с сервером", "Unknow API response:\n"+resultText);
                if (res.reload)  reloadPage();
            }
            catch (e)  {  error("Ошибка взаимодействия с сервером", e);  }
        },
        function (e)  {
            error("Ошибка взаимодействия с сервером", e);
        });
    }
    catch (e)
    {
        error("Ошибка взаимодействия с сервером", e);
    }

}