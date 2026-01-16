package org.hasting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller that forwards non-API requests to the React frontend.
 *
 * <p>This controller implements Single Page Application (SPA) routing support,
 * ensuring that client-side routes work correctly when accessed directly or
 * when the page is refreshed. Without this, accessing routes like /duplicates
 * directly would result in a 404 error.
 *
 * <p>The pattern excludes:
 * <ul>
 * <li>API routes (/api/*)</li>
 * <li>Static resources (files with extensions like .js, .css, .png)</li>
 * <li>WebSocket connections (/ws/*)</li>
 * </ul>
 *
 * @since 2.0.0
 */
@Controller
public class SpaController {

    /**
     * Forwards requests to index.html for client-side routing.
     *
     * <p>The regex pattern matches paths that:
     * <ul>
     * <li>Don't contain a dot (excludes static files like .js, .css)</li>
     * <li>Are not API endpoints (those are handled by @RestController)</li>
     * </ul>
     *
     * @return forward instruction to index.html
     */
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String forwardToIndex() {
        return "forward:/index.html";
    }

    /**
     * Handles nested routes (e.g., /music/123, /settings/profiles).
     *
     * @return forward instruction to index.html
     */
    @RequestMapping(value = "/{path1:[^\\.]*}/{path2:[^\\.]*}")
    public String forwardNestedToIndex() {
        return "forward:/index.html";
    }

    /**
     * Handles deeply nested routes (e.g., /settings/profiles/123).
     *
     * @return forward instruction to index.html
     */
    @RequestMapping(value = "/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}")
    public String forwardDeeplyNestedToIndex() {
        return "forward:/index.html";
    }
}
