<!DOCTYPE html>
<html>
<head>
    <title>GraphiQL</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/graphiql/graphiql.min.css" />
</head>
<body style="margin:0;">
<div id="graphiql" style="height:100vh;"></div>
<script src="https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/graphiql/graphiql.min.js"></script>
<script>
    const paths = window.location.pathname.split('/');
    const hub = paths[paths.length - 1];
    const fetcher = GraphiQL.createFetcher({ url: '/ws/igraphql/' + hub });
    ReactDOM.render(
        React.createElement(GraphiQL, { fetcher }),
        document.getElementById('graphiql'),
    );
</script>
</body>
</html>