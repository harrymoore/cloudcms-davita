define(function (require, exports, module) {
    var $ = require("jquery");

    var BASE_URL = "http://localhost.com:8080/documents/";
    var rgx = /\/documents\/(?<doc>[^/]+)/;

    $(document).on('cloudcms-ready', function (ev) {
        var url = window.location.href;
        if (url && -1 !== url.indexOf('/content/davita:document/documents/')) {
            var found = url.match(rgx);
            var  appUrl = BASE_URL + found.groups.doc || "";

            // insert an anchor link and copy button
            $('#hud > div > div:nth-child(1) > div > div > div > div').append(`
                <a href="${appUrl}" target="redirected"><button class="btn btn-default">Open In App</button></a>
                <button class="btn btn-default" id="copy-link" data-clipboard-action="copy" data-clipboard-target="#app-link">Copy App Link</button>
                <p hidden id="app-link" class="app-link">${appUrl}</p>
            `);

            $('#copy-link').on('click', function (event) {
                // http://localhost.com:8080/documents/x
                var el = document.createElement('textarea');
                el.value = appUrl;
                document.body.appendChild(el);
                el.select();
                document.execCommand('copy');
                document.body.removeChild(el);
            });
        }
    });

    // use larger image previews
    document.addEventListener("load", function (ev) {
        var s = "" + ev.target.src;
        if (s && -1 !== s.indexOf('.cloudcms.net/preview/') && -1 !== s.indexOf('name=icon64') && -1 !== s.indexOf('size=64')) {
            s = s.replace("name=icon64", "name=iconbig");
            s = s.replace("size=64", "size=128");
            s = s.replace("mimetype=image/png", "mimetype=image/jpeg");
            ev.target.src = s;
        }
    }, true);

});