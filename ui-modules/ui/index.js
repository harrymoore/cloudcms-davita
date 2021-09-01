try {
    define(function(require, exports, module) {
        var moduleUrl = module.uri.match(/^.+(_modules[^\/]+)\/.*/)[1];

        require(`${moduleUrl}/fields/epoch-date-field.js`);
        require(`${moduleUrl}/scripts/mods.js`);
        require(`${moduleUrl}/scripts/training.js`);
        require(`${moduleUrl}/scripts/help.js`);
    });
} finally {}
