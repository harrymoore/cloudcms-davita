define(function(require, exports, module) {
    var $ = require("jquery");
    var Alpaca = $.alpaca;

    Alpaca.Fields.EpochDateField = Alpaca.Fields.DateField.extend(
    {
        _backingValue: null,

        onConstruct: function()
        {
            var self = this;
            self.base();

            if (self.data) {
                self._backingValue = self.data;
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
            var value = self._backingValue;

            return value;
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

