/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class', '[data-theme="dark"]'],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "rgb(var(--background) / <alpha-value>)",
        surface: "rgb(var(--surface) / <alpha-value>)",
        "surface-container": "rgb(var(--surface-container) / <alpha-value>)",
        "surface-container-high": "rgb(var(--surface-container-high) / <alpha-value>)",
        primary: "rgb(var(--primary) / <alpha-value>)",
        "primary-foreground": "rgb(var(--primary-foreground) / <alpha-value>)",
        secondary: "rgb(var(--secondary) / <alpha-value>)",
        accent: "rgb(var(--accent) / <alpha-value>)",
        muted: "rgb(var(--muted) / <alpha-value>)",
        border: "rgb(var(--border) / <alpha-value>)",
        foreground: "rgb(var(--foreground) / <alpha-value>)",
        "gemini-blue": "#667eea",
      },
      fontFamily: {
        sans: [
          '"Google Sans"',
          '"Inter"',
          '"Roboto"',
          'system-ui',
          'sans-serif'
        ],
        display: [
          '"Google Sans Display"',
          '"Google Sans"',
          'sans-serif'
        ],
        mono: [
          '"JetBrains Mono"',
          '"Fira Code"',
          '"Roboto Mono"',
          'monospace'
        ]
      },
      animation: {
        "fade-in": "fadeIn 0.4s cubic-bezier(0.2, 0.0, 0, 1.0)",
        "slide-up": "slideUp 0.5s cubic-bezier(0.2, 0.0, 0, 1.0)",
        "scale-in": "scaleIn 0.3s cubic-bezier(0.2, 0.0, 0, 1.0)",
        "shimmer": "shimmer 2s infinite",
        "float": "float 3s ease-in-out infinite",
        "pulse-glow": "pulseGlow 2s ease-in-out infinite",
        "breathe": "breathe 3s ease-in-out infinite",
        "gradient": "gradientShift 4s ease infinite",
        "glow": "glow 2s ease-in-out infinite alternate",
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        slideUp: {
          "0%": { transform: "translateY(20px)", opacity: "0" },
          "100%": { transform: "translateY(0)", opacity: "1" },
        },
        scaleIn: {
          "0%": { transform: "scale(0.95)", opacity: "0" },
          "100%": { transform: "scale(1)", opacity: "1" },
        },
        shimmer: {
          "0%": { backgroundPosition: "-200% 0" },
          "100%": { backgroundPosition: "200% 0" },
        },
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-8px)" },
        },
        pulseGlow: {
          "0%, 100%": {
            boxShadow: "0 0 20px rgba(var(--primary), 0.4)",
            opacity: "1"
          },
          "50%": {
            boxShadow: "0 0 40px rgba(var(--primary), 0.6)",
            opacity: "0.9"
          },
        },
        breathe: {
          "0%, 100%": { transform: "scale(1)", opacity: "1" },
          "50%": { transform: "scale(1.05)", opacity: "0.85" },
        },
        gradientShift: {
          "0%, 100%": { backgroundPosition: "0% center" },
          "50%": { backgroundPosition: "100% center" },
        },
        glow: {
          "0%": { boxShadow: "0 0 20px rgba(var(--primary), 0.3)" },
          "100%": { boxShadow: "0 0 30px rgba(var(--primary), 0.5)" },
        }
      },
      backgroundImage: {
        'gemini-gradient': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'gemini-gradient-text': 'linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)',
        'premium-gradient': 'linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)',
        'accent-gradient': 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'glow-radial': 'radial-gradient(ellipse at center, rgba(99, 102, 241, 0.15) 0%, transparent 70%)',
      },
      boxShadow: {
        'glow-sm': '0 0 15px rgba(var(--primary) / 0.2)',
        'glow-md': '0 0 25px rgba(var(--primary) / 0.3)',
        'glow-lg': '0 0 40px rgba(var(--primary) / 0.4)',
        'premium': '0 10px 40px rgba(0, 0, 0, 0.12), 0 4px 12px rgba(0, 0, 0, 0.08)',
        'premium-hover': '0 20px 60px rgba(0, 0, 0, 0.15), 0 8px 24px rgba(0, 0, 0, 0.1)',
      },
      backdropBlur: {
        xs: '2px',
      },
      borderRadius: {
        '4xl': '2rem',
      }
    },
  },
  plugins: [],
}
