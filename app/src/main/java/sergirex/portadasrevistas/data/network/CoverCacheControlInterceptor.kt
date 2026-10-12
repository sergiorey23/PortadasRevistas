package sergirex.portadasrevistas.data.network

import okhttp3.Interceptor
import okhttp3.Response
import sergirex.portadasrevistas.domain.model.CoverUrls

/**
 * Cover and page images of a specific issue (addressed by magazine id + date in the URL) never
 * change once published, but the image host sends no usable freshness headers, so Coil would fall
 * back to a heuristic and re-download unpredictably. They're cached for a year instead.
 *
 * The "latest cover" URLs on the kiosk host are left alone: they change when a new issue comes
 * out, and that server already sends a sensible `max-age`.
 */
class CoverCacheControlInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.url.host != CoverUrls.PAGES_HOST || (!response.isSuccessful && response.code != 304)) {
            return response
        }
        if (response.header("Cache-Control") == ISSUE_IMAGE) return response
        return response.newBuilder().header("Cache-Control", ISSUE_IMAGE).build()
    }

    private companion object {
        const val ISSUE_IMAGE = "public, max-age=31536000, immutable"
    }
}
