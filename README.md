# Playing with WebGL

Just playing with WebGL using ClojureScript.

## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).

To create a production build run:

    lein dist

## Issues

Works on Chrome and Firefox (at least on Mac), on Safari the alpha does not work. On iOS Safari does
not show anything. I can see that the touch events work, but the WebGL rendering does not show anything. 

## License

Copyright © 2018 Jarppe Länsiö

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
