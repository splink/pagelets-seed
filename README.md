[![Build Status](https://travis-ci.org/splink/pagelets-seed.svg?branch=master)](https://travis-ci.org/splink/pagelets-seed)

# Hello Pagelets!
just a few minutes and you have a complete Play application based on the [Play Pagelets Module](https://github.com/splink/pagelets) running.

See the app in action: [Pagelets on Heroku](https://pagelets.herokuapp.com/) 
*note that it runs on a free dyno which takes a couple of seconds to wake up.

## TL;DR
The demo application serves the purpose to
 - showcase the Play Pagelets Module and it's advantages
 - demonstrate how to build a pagelet based application
 - serve as a template for pagelet based projects


## What is a Pagelet
A pagelet is a small independent unit which consists of view, controller action and optionally a service which obtains the data to be rendered. 
Usually a web page is composed of multiple pagelets.


## The demo app
This demo application shows how a modular and resilient web application can be built using the 
[Playframework 2](http://www.playframework.com) and the [Play Pagelets Module](https://github.com/splink/pagelets).

This example app is a small multi-language website. The page is composed of multiple pagelets. Each pagelet is completely 
independent and obtains data from a remote service and renders the data by means of a standard twirl template. 

Depending on the selected language, the home page is composed from different pagelets. This shows how the page composition 
can be manipulated at runtime based on the properties of an incoming request.

The demo also invites to to simulate failure by configuring remote services to time-out or serve broken data.


## To get a rough idea what the pagelet API looks like:

A page configuration
~~~scala
def tree(r: RequestHeader) = 
  Tree('root, Seq(
    Leaf('header, header _).withJavascript(Javascript("lib/bootstrap/js/dropdown.min.js")),
    Tree('content, Seq(
      Leaf('carousel, carousel _).withFallback(fallback("Carousel") _),
      Leaf('text, text _).withFallback(fallback("Text") _)
    )),
    Leaf('footer, footer _).withCss(Css("stylesheets/footer.min.css"))
  ))
~~~

A main action to render a complete page
~~~scala
def index = PageAction.async(routes.HomeController.errorPage)(_ => "Page Title", tree) { (request, page) =>
  views.html.wrapper(routes.HomeController.resourceFor)(page)
}
~~~

A pagelet (just a standard Play action)
~~~scala
def carousel = Action.async { implicit request =>
  carouselService.carousel.map { teasers =>
    Ok(views.html.pagelets.carousel(teasers))
  }
}
~~~

A fallback pagelet (also just a standard Play action)
~~~scala
def fallback(name: String)() = Action {
  Ok(views.html.pagelets.fallback(name))
}
~~~

## Getting started
If you have [activator](https://www.lightbend.com/community/core-tools/activator-and-sbt#overview) installed, just enter:

~~~bash
activator new
play-pagelets-seed
~~~

Otherwise open a terminal and

- clone the github repository with
~~~bash
git clone git@github.com:splink/pagelets-seed.git
~~~

- then enter
~~~bash
cd play-pagelets-seed
~~~

- then enter
~~~bash
sbt run
~~~

then point your browser to [http://localhost:9000](http://localhost:9000)


If you are interested in more details, check out the main [Play Pagelets repository](https://github.com/splink/pagelets)
