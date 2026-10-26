const isEnvironmentProduction = () => {
    const env = (process.env.DEPLOYMENT_ENVIRONMENT || '').trim().toLowerCase();
    return env === 'prod' || env === 'production';
}

const hasValue = (v) => v != null && v !== "";

module.exports = {
    isEnvironmentProduction,
    hasValue,
}
