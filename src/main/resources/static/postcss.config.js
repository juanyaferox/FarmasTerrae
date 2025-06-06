const postcssConfig = {
    plugins: [
        require('@tailwindcss/postcss')
    ]
};

// If we are in production mode, then add cssnano
    postcssConfig.plugins.push(
        require('cssnano')({
            // use the safe preset so that it doesn't
            // mutate or remove code from our css
            preset: 'default',
        })
    );

module.exports = postcssConfig;