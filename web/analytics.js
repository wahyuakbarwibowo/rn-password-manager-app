// analytics.js - Client-side analytics tracker for Aminmart Password Manager website
// Uses localStorage to store analytics data in the browser

class SimpleAnalytics {
  constructor() {
    this.storageKey = 'aminmart_analytics';
    this.currentPage = this.getCurrentPage();
    this.init();
  }

  getCurrentPage() {
    // Get the current page name from the URL
    const path = window.location.pathname;
    const page = path.split('/').pop() || 'index.html';
    return page;
  }

  init() {
    // Track page view when the page loads
    this.trackPageView();
    
    // Add event listeners to download buttons
    this.addDownloadButtonListeners();
  }

  trackPageView() {
    const analyticsData = this.getAnalyticsData();
    
    // Increment page view count for current page
    if (analyticsData.page_views[this.currentPage] !== undefined) {
      analyticsData.page_views[this.currentPage]++;
    } else {
      analyticsData.page_views[this.currentPage] = 1;
    }
    
    // Increment total page views
    analyticsData.total_page_views++;
    
    // Update last updated timestamp
    analyticsData.last_updated = new Date().toISOString();
    
    // Save updated data to localStorage
    this.saveData(analyticsData);
    
    // Update UI with current counts
    this.updateUI(analyticsData);
  }

  trackDownloadClick() {
    const analyticsData = this.getAnalyticsData();
    
    // Increment download click count for current page
    if (analyticsData.download_clicks[this.currentPage] !== undefined) {
      analyticsData.download_clicks[this.currentPage]++;
    } else {
      analyticsData.download_clicks[this.currentPage] = 1;
    }
    
    // Increment total download clicks
    analyticsData.total_download_clicks++;
    
    // Update last updated timestamp
    analyticsData.last_updated = new Date().toISOString();
    
    // Save updated data to localStorage
    this.saveData(analyticsData);
    
    // Update UI with current counts
    this.updateUI(analyticsData);
  }

  getAnalyticsData() {
    const stored = localStorage.getItem(this.storageKey);
    if (stored) {
      return JSON.parse(stored);
    }
    
    // Return default structure if no data exists
    return {
      page_views: {
        "index.html": 0,
        "contact.html": 0,
        "delete-account.html": 0,
        "support-id.html": 0,
        "support-en.html": 0
      },
      download_clicks: {
        "index.html": 0,
        "contact.html": 0,
        "delete-account.html": 0,
        "support-id.html": 0,
        "support-en.html": 0
      },
      last_updated: new Date().toISOString(),
      total_page_views: 0,
      total_download_clicks: 0
    };
  }

  saveData(data) {
    localStorage.setItem(this.storageKey, JSON.stringify(data));
  }

  addDownloadButtonListeners() {
    // Find all download buttons (those with 'download' in the class or text)
    const downloadButtons = document.querySelectorAll('a[href*="apk"], button[id*="download"], a[class*="download"]');
    
    downloadButtons.forEach(button => {
      button.addEventListener('click', (event) => {
        // Check if the button is actually a download button
        if (button.href.includes('apk') || 
            button.classList.contains('download') || 
            button.id.includes('download') ||
            button.textContent.toLowerCase().includes('download')) {
          this.trackDownloadClick();
        }
      });
    });
  }

  updateUI(data) {
    // Update any analytics display elements on the page
    const pageViewsElement = document.getElementById('page-views-count');
    const downloadCountElement = document.getElementById('download-count');
    
    if (pageViewsElement) {
      pageViewsElement.textContent = data.page_views[this.currentPage] || 0;
    }
    
    if (downloadCountElement) {
      downloadCountElement.textContent = data.download_clicks[this.currentPage] || 0;
    }
    
    // Also update total counts if elements exist
    const totalPagesElement = document.getElementById('total-page-views');
    const totalDownloadsElement = document.getElementById('total-downloads');
    
    if (totalPagesElement) {
      totalPagesElement.textContent = data.total_page_views || 0;
    }
    
    if (totalDownloadsElement) {
      totalDownloadsElement.textContent = data.total_download_clicks || 0;
    }
    
    // Update last updated time if element exists
    const lastUpdatedElement = document.getElementById('last-updated');
    if (lastUpdatedElement) {
      const date = new Date(data.last_updated);
      lastUpdatedElement.textContent = date.toLocaleString();
    }
  }
}

// Initialize analytics when the page loads
document.addEventListener('DOMContentLoaded', function() {
  // Check if we're in a browser environment
  if (typeof window !== 'undefined') {
    // Initialize the analytics tracker
    const analytics = new SimpleAnalytics();
    
    // Store the instance globally so it can be accessed if needed
    window.aminmartAnalytics = analytics;
  }
});