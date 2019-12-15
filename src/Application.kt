package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.thymeleaf.Thymeleaf
import io.ktor.thymeleaf.ThymeleafContent
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import io.ktor.features.*
import io.ktor.auth.*
import io.ktor.auth.jwt.jwt
import io.ktor.gson.*
import io.ktor.client.*
import org.thymeleaf.templateparser.text.TextParseException
import java.lang.Exception

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/thymeleaf/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost()
    }
    val simpleJWT = SimpleJWT("test")
    install(Authentication) {
        jwt {
            verifier {

                simpleJWT.verifier
            }
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }

    }

    install(ContentNegotiation) {
        gson {
        }
    }

    install(StatusPages) {
//            exception<AuthorizationException> { cause ->
//                call.respond(HttpStatusCode.Forbidden)
//            }

        exception<Exception> {
                exc ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("ok" to false,"error" to (exc.message?:"")))


        }


    }


//    val client = HttpClient(object :HttpClientConfig<String>{}) {
//        engine {
//            threadsCount = 4
//            pipelining =true
//        }
//    }
    intercept(ApplicationCallPipeline.Call){
        if (!call.request.uri.contains("wb")){
            call.respond("未实现")
        }
    }

    routing {

        route("/login") {
            post("/a") {
//                throw TestException("登陆异常")
                val params = call.receive<TestClass>()
                log.debug("a:${params.a}")
                log.debug("b:${params.b}")

                call.respond(mapOf("token" to simpleJWT.sign(params.a ?: "测试用户")))
            }
            authenticate {
                post {
                    val p = call.principal<UserIdPrincipal>() ?: error("message" to "认证未通过")

                    log.debug("当前用户:${p.name}")
//                    throw RuntimeException("aaaaa")
                    call.respond(mapOf("state" to "用户${p.name}认证成功"))
                }
            }
        }


        get("/html-dsl") {
            call.respondHtml {
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }

        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.red
                }
                p {
                    fontSize = 2.em
                }
                rule("p.myclass") {
                    color = Color.blue
                }
            }
        }

        get("/html-thymeleaf") {
            call.respond(ThymeleafContent("index", mapOf("user" to ThymeleafUser(1, "user1"))))
        }



        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

data class ThymeleafUser(val id: Int, val name: String)


fun FlowOrMetaDataContent.styleCss(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        +CSSBuilder().apply(builder).toString()
    }
}

fun CommonAttributeGroupFacade.style(builder: CSSBuilder.() -> Unit) {
    this.style = CSSBuilder().apply(builder).toString().trim()
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String {
        return JWT.create().withClaim("name", name).sign(algorithm)
    }
}

data class TestClass(val a: String, val b: Int)
class TestException(message: String) : RuntimeException(message)