const path = require('path');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");


module.exports = {
    mode: 'production',
    entry: {
        // tools: {
        //     import:'./src/main/webapp/js/tools.js',
        //     dependOn: 'vendor'
        // },
        // score: {
        //     import:'./src/main/webapp/js/score.js',
        //     dependOn: 'vendor',
        //     library: {
        //         type:'assign',
        //         name:'Ecodata'
        //     }
        // },

        vendor:[
            'jquery',
            'knockout',
            'jsoneditor',
            'bootstrap',
            'bootstrap-datepicker',
            './src/main/webapp/vendor/jquery-validation-engine/jquery.validationEngine.js',
            './src/main/webapp/vendor/jquery-validation-engine/jquery.validationEngine-en.js',
            'knockout-mapping',
            'knockout-sortable',
            'jquery-ui',
            'lodash',
            'select2'
        ],
        ecodata:'./src/main/webapp/css/ecodata.css'
    },
    devtool: 'inline-source-map',
    output: {
        path: path.join(__dirname, 'grails-app/assets/dist/'),
        publicPath: '/assets/',
        filename: 'bundle-[name].js',
        assetModuleFilename: '[name][ext]',
        clean: true,
    },
    optimization: {
        runtimeChunk: 'single'
    },
    module: {

        rules: [
            {
                test: /\.css$/,
                use: [MiniCssExtractPlugin.loader,
                    "css-loader"
                ]
            },
            {
                test: /\.(jpe?g|png|gif|svg|eot|woff|woff2|ttf)$/i,
                type:'asset/resource'
            },
            {
                test: require.resolve("jquery"),
                loader: "expose-loader",
                options: {
                    exposes: ["$", "jQuery"]
                },
            },
            {
                test: require.resolve("knockout"),
                loader: "expose-loader",
                options: {
                    exposes: ["ko"]
                },
            },
            {
                test: require.resolve("jsoneditor"),
                loader: "expose-loader",
                options: {
                    exposes: ["JSONEditor"]
                },
            },
        ]
    },
    plugins: [
        new MiniCssExtractPlugin({filename:'[name].css'}),
    ],
    target: ['web', 'es5']

};