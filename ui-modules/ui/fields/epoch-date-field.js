define(function(require, exports, module) {
    var $ = require("jquery");
    var Alpaca = $.alpaca;

    const millisecondsPerMinute = 60000;
    const millisecondsPerDay = 8.64e+7;

    Alpaca.Fields.EpochDateField = Alpaca.Fields.DateField.extend(
    {
        _backingValue: null,

        onConstruct: function()
        {
            var self = this;
            self.base();

            if (self.data) {
                self._backingValue = Math.ceil(self.data * millisecondsPerDay);
                self.data = moment(self._backingValue).format("L");
            }
        },

        setup: function()
        {
            var self = this;
            self.base();
        },

        getControlValue: function()
        {
            var self = this;
            var val = null;

            if (self.control)
            {
                val = $(self.control).val();
            }

            return val;    
        },

        getValue: function()
        {
            var self = this;
            if (self._backingValue) {
                return Math.ceil(self._backingValue / millisecondsPerDay);
            }

            return null;
        },

        setValue: function(value)
        {
            var self = this;
            self.base(value);
        },

        onChange: function(e)
        {
            var self = this;
            self._backingValue = (new Date(self.getControlValue())).getTime();
            self.base(e);
        },

        getFieldType: function() {
            return "epoch-datetime";
        },
 
    });
    
    Alpaca.registerFieldClass("epoch-date", Alpaca.Fields.EpochDateField);
});

