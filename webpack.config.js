const path = require('path');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");


module.exports = {
    mode: 'production',
    entry: {
        tools: {
            import:'./src/main/webapp/js/tools.js',
            dependOn: 'vendor'
        },
        vendor:['jquery', 'knockout'],
        ecodata:'./src/main/webapp/css/ecodata.css'
    },
    devtool: 'inline-source-map',
    output: {
        path: path.join(__dirname, 'grails-app/assets/dist/'),
        publicPath: '/assets/',
        filename: 'bundle-[name].js',
        assetModuleFilename: '[name][ext]',
        clean: true
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
            }
        ]
    },
    plugins: [
        new MiniCssExtractPlugin({filename:'[name].css'}),
    ],
    target: ['web', 'es5']

};