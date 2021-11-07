const path = require('path');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");


module.exports = {
    mode: 'production',
    entry: {
        tools: {
            import:'./src/main/webapp/js/tools.js',
            dependOn: 'vendor'

        },
        score: {
            import:'./src/main/webapp/js/score.js',
            dependOn: 'vendor',
            library: {
                type:'assign',
                name:'Ecodata'
            }
        },
        vendor:['jquery', 'knockout', 'jsoneditor'],
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
        ]
    },
    plugins: [
        new MiniCssExtractPlugin({filename:'[name].css'}),
    ],
    target: ['web', 'es5']

};