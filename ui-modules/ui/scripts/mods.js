define(function (require, exports, module) {
    var $ = require("jquery");

    var BASE_URL_PATTERN = "http://localhost.com:8080/documents/{{document.id}}?clearCache=true";
    var rgx = /\/documents\/(?<doc>[^/]+)/;

    $(document).on('cloudcms-ready', function (ev) {
        var url = window.location.href;
        if ($('#document-summary a').attr('href') && -1 !== $('#document-summary a').attr('href').indexOf('/davita:document')) {
            var found = url.match(rgx);
            var appUrl = "";
        
            if (found && found.groups && found.groups.doc) {
                var previews = window.Ratchet.observable("project").get().previews;
                if (previews) {
                    previews.forEach(function(preview) {
                        if (preview.id === "production") {
                            BASE_URL_PATTERN = preview.url;
                        }
                    });
                }

                appUrl = BASE_URL_PATTERN.replace("{{document.id}}", found.groups.doc);
            }

            // insert an anchor link and copy button
            if (!$('#copy-link').length) {
                $('#hud > div > div:nth-child(1) > div > div > div > div').append(`
                    <button class="btn btn-default" id="copy-link" data-clipboard-action="copy" data-clipboard-target="#app-link">Copy App Link</button>
                `);

                $('#copy-link').on('click', function (event) {
                    var el = document.createElement('textarea');
                    el.value = appUrl;
                    document.body.appendChild(el);
                    el.select();
                    document.execCommand('copy');
                    document.body.removeChild(el);
                });
                    
            }
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