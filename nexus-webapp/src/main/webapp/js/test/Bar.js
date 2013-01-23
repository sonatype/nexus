Ext.define('test.Bar', {
    extend: 'test.Foo',
    //requires: [ 'ext/string', 1 ],
    //requirejs: [ 'ext/string', 1 ],
    //requireSuper: false,

    constructor: function (config) {
        var self = this;

        // Call super constructor
        //self.constructor.superclass.constructor.apply(self, arguments);
    }
});

