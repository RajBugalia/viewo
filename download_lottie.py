import urllib.request
import re

def search_ddg(query):
    url = 'https://html.duckduckgo.com/html/?q=' + urllib.parse.quote(query)
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        html = urllib.request.urlopen(req).read().decode('utf-8')
        return html
    except Exception as e:
        print('Error fetching DDG:', e)
        return ''

html = search_ddg('site:raw.githubusercontent.com lottie tv json')
links = re.findall(r'https://raw\.githubusercontent\.com[^\'\" >]*tv[^\'\" >]*\.json', html, re.IGNORECASE)

if not links:
    html = search_ddg('site:raw.githubusercontent.com lottie screen json')
    links = re.findall(r'https://raw\.githubusercontent\.com[^\'\" >]*screen[^\'\" >]*\.json', html, re.IGNORECASE)

if not links:
    html = search_ddg('site:raw.githubusercontent.com lottie monitor json')
    links = re.findall(r'https://raw\.githubusercontent\.com[^\'\" >]*monitor[^\'\" >]*\.json', html, re.IGNORECASE)

if links:
    link = links[0]
    print('Found URL:', link)
    urllib.request.urlretrieve(link, 'app/src/main/res/raw/splash.json')
    print('Downloaded to splash.json')
else:
    print('No links found in DDG')
    
    # fallback to a known lottie (though not tv, it's better than nothing, maybe a loader)
    fallback = 'https://raw.githubusercontent.com/LottieFiles/lottie-react-native/main/example/assets/PinJump.json'
    urllib.request.urlretrieve(fallback, 'app/src/main/res/raw/splash.json')
    print('Downloaded fallback PinJump')
