package com.grailsrocks.cacheheaders

import java.text.SimpleDateFormat

import grails.test.*

import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest

import com.grailsrocks.cacheheaders.*

class CacheHeadersServiceTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
        mockLogging(CacheHeadersService)
    }

    protected void tearDown() {
        super.tearDown()
    }

    static RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz" // Always GMT

    String dateToHTTPDate(date) {

        def v = new SimpleDateFormat(RFC1123_DATE_FORMAT, Locale.ENGLISH)
        v.timeZone = TimeZone.getTimeZone('GMT')
        return v.format(date)
    }
    
    void testWithCacheHeadersCachingDisabled() {
        def svc = new CacheHeadersService()
        svc.enabled = false
        
        def req = new MockHttpServletRequest()
        req.addHeader('If-None-Match', "1234567Z")
        
        def resp = new MockHttpServletResponse()
        
        def context = new Expando(
            request: req,
            response: resp
        )
        context.render = { String s -> resp.outputStream << s.bytes }
            
        def res = svc.withCacheHeaders(context) {
            etag {
                "1234567Z"
            }
            generate {
                render "Hello!"
            }
        }
        
        assertEquals 200, resp.status
        assertEquals "Hello!", resp.contentAsString
        assertNull resp.getHeader('Last-Modified')
        assertNull resp.getHeader('ETag')
    }

    void testWithCacheHeadersETagMatch() {
        def svc = new CacheHeadersService()

        def req = new MockHttpServletRequest()
        req.addHeader('If-None-Match', "1234567Z")
        
        def resp = new MockHttpServletResponse()
        
        def context = new Expando(
            request: req,
            response: resp
        )
        context.render = { String s -> resp.outputStream << s.bytes }
            
        def res = svc.withCacheHeaders(context) {
            etag {
                "1234567Z"
            }
        }
        
        assertEquals 304, resp.status
        assertNull resp.getHeader('Last-Modified')
        assertNull resp.getHeader('ETag')
    }

    void testWithCacheHeadersETagNoMatchLastModUnchanged() {
        def svc = new CacheHeadersService()
        
        def lastMod = new Date()-100
        
        def req = new MockHttpServletRequest()
        req.addHeader('If-None-Match', "dsfdsfdsfdsfsd")
        // This is an AWFUL hack because spring mock http request/response does not do string <-> date coercion
        req.addHeader('If-Modified-Since', lastMod)
        
        def resp = new MockHttpServletResponse()
        
        def context = new Expando(
            request: req,
            response: resp
        )
        context.render = { String s -> resp.outputStream << s.bytes }
            
        def res = svc.withCacheHeaders(context) {
            etag {
                "1234567Z"
            }
            
            lastModified {
                lastMod
            }
            
            generate {
                render "Derelict Herds"
            }
        }
        
        assertEquals 200, resp.status
        assertEquals "Derelict Herds", resp.contentAsString
        assertEquals lastMod.time, resp.getHeader('Last-Modified')
        assertEquals "1234567Z", resp.getHeader('ETag')
    }

    void testWithCacheHeadersETagMatchLastModChanged() {
        def svc = new CacheHeadersService()
        
        def lastMod = new Date()-100
        
        def req = new MockHttpServletRequest()
        req.addHeader('If-None-Match', "bingo")
        // This is an AWFUL hack because spring mock http request/response does not do string <-> date coercion
        req.addHeader('If-Modified-Since', lastMod-1)
        
        def resp = new MockHttpServletResponse()
        
        def context = new Expando(
            request: req,
            response: resp
        )
        context.render = { String s -> resp.outputStream << s.bytes }
            
        def res = svc.withCacheHeaders(context) {
            etag {
                "bingo"
            }
            
            lastModified {
                lastMod
            }
            
            generate {
                render "Derelict Herds"
            }
        }
        
        assertEquals 200, resp.status
        assertEquals "Derelict Herds", resp.contentAsString
        assertEquals lastMod.time, resp.getHeader('Last-Modified')
        assertEquals "bingo", resp.getHeader('ETag')
    }

    void testWithCacheHeadersETagMatchLastModUnchanged() {
        def svc = new CacheHeadersService()
        
        def lastMod = new Date()-100
        
        def req = new MockHttpServletRequest()
        req.addHeader('If-None-Match', "bingo")
        // This is an AWFUL hack because spring mock http request/response does not do string <-> date coercion
        req.addHeader('If-Modified-Since', lastMod)
        
        def resp = new MockHttpServletResponse()
        
        def context = new Expando(
            request: req,
            response: resp
        )
        context.render = { String s -> resp.outputStream << s.bytes }
            
        def res = svc.withCacheHeaders(context) {
            etag {
                "bingo"
            }
            
            lastModified {
                lastMod
            }
            
            generate {
                render "Derelict Herds"
            }
        }
        
        assertEquals 304, resp.status
    }

    void testWithCacheHeadersETagNoMatchLastModChanged() {
        def svc = new CacheHeadersService()
        
        def lastMod = new Date()-100
        
        def req = new MockHttpServletRequest()
        req.addHeader('If-None-Match', "dsfdsfdsfdsfsd")
        // This is an AWFUL hack because spring mock http request/response does not do string <-> date coercion
        req.addHeader('If-Modified-Since', lastMod-1)
        
        def resp = new MockHttpServletResponse()
        
        def context = new Expando(
            request: req,
            response: resp
        )
        context.render = { String s -> resp.outputStream << s.bytes }
            
        def res = svc.withCacheHeaders(context) {
            etag {
                "1234567Z"
            }
            
            lastModified {
                lastMod
            }
            
            generate {
                render "Derelict Herds"
            }
        }
        
        assertEquals 200, resp.status
        assertEquals "Derelict Herds", resp.contentAsString
        assertEquals lastMod.time, resp.getHeader('Last-Modified')
        assertEquals "1234567Z", resp.getHeader('ETag')
    }

    void testWithCacheHeadersLastModChanged() {
        def svc = new CacheHeadersService()

        def req = new MockHttpServletRequest()
        // This is an AWFUL hack because spring mock http request/response does not do string <-> date coercion
        req.addHeader('If-Modified-Since', new Date()-102)
        
        def resp = new MockHttpServletResponse()
        
        def lastMod = new Date()-100
        
        def context = new Expando(
            request: req,
            response: resp,
            render: { String s -> resp.outputStream << s.bytes }
        )
            
        def res = svc.withCacheHeaders(context) {
            etag {
                "OU812"
            }
            lastModified {
                lastMod
            }
            
            generate {
                render "Porcelain Heart"
            }
        }
        
        assertEquals 200, resp.status
        assertEquals "Porcelain Heart", resp.contentAsString
        assertEquals lastMod.time, resp.getHeader('Last-Modified')
        assertEquals "OU812", resp.getHeader('ETag')
    }

    void testWithCacheHeadersLastModNotNewer() {
        def svc = new CacheHeadersService()

        def d = new Date()-100
        def req = new MockHttpServletRequest()
        // This is an AWFUL hack because spring mock http request/response does not do string <-> date coercion
        req.addHeader('If-Modified-Since', d)
        
        def resp = new MockHttpServletResponse()
        
        def lastMod = d
        
        def context = new Expando(
            request: req,
            response: resp,
            render: { String s -> resp.outputStream << s.bytes }
        )
            
        def res = svc.withCacheHeaders(context) {
            etag {
                "5150"
            }
            lastModified {
                lastMod
            }
            
            generate {
                render "Hessian Peel"
            }
        }
        
        assertEquals 304, resp.status
        assertNull resp.getHeader('Last-Modified')
        assertNull resp.getHeader('ETag')
    }
}
