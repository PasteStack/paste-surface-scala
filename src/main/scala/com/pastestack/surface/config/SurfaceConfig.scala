package com.pastestack.surface.config

import com.pastestack.surface.view_model_factories.{PageMetaFactory, IncludesFactory}

/**
 * Configuration loader for paste-surface.
 * 
 * A consuming site stores its configuration in resources/paste-surface.conf
 * and loads it once at startup.
 * 
 * == Usage ==
 * {{{
 * // In your app's bootstrap/main:
 * val config = SurfaceConfig.load()
 * 
 * // Then use in routes/templates:
 * val pageMeta = PageMetaFactory.forHomePage(config.site)
 * val jsIncludes = IncludesFactory.forPasteJam()
 * }}}
 */
case class SurfaceConfig(
  site: PageMetaFactory.SiteConfig,
  assets: IncludesFactory.AssetConfig
)

object SurfaceConfig {
  
  /**
   * Load from Typesafe Config (application.conf or paste-surface.conf).
   * 
   * Requires: "com.typesafe" % "config" dependency
   */
  def fromTypesafeConfig(config: com.typesafe.config.Config): SurfaceConfig = {
    val ps = config.getConfig("paste.surface")
    
    SurfaceConfig(
      site = PageMetaFactory.SiteConfig(
        siteName = ps.getString("site.name"),
        domainName = ps.getString("site.domain"),
        themeColor = ps.getString("site.theme-color"),
        defaultDescription = ps.getString("site.default-description"),
        defaultKeywords = ps.getString("site.default-keywords")
      ),
      assets = IncludesFactory.AssetConfig(
        debugMode = ps.getBoolean("assets.debug-mode")
      )
    )
  }
  
  /**
   * Load from default config file.
   */
  def load(): SurfaceConfig = {
    import com.typesafe.config.ConfigFactory
    fromTypesafeConfig(ConfigFactory.load("paste-surface"))
  }
  
  /**
   * Create directly (for simple sites or tests).
   */
  def create(
    siteName: String,
    domainName: String,
    themeColor: String = "",
    debugMode: Boolean = false
  ): SurfaceConfig = SurfaceConfig(
    site = PageMetaFactory.SiteConfig(siteName, domainName, themeColor),
    assets = IncludesFactory.AssetConfig(debugMode)
  )
}
