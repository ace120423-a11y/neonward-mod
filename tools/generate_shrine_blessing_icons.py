"""Small original pixel-art icons for the two timed shrine effects."""
from pathlib import Path
from PIL import Image, ImageDraw

target = Path(__file__).resolve().parents[1] / 'resources/assets/neonward/textures/mob_effect'
target.mkdir(parents=True, exist_ok=True)
for kind, color in [('attack', '#ee974e'), ('defense', '#62b8d5')]:
    image = Image.new('RGBA', (18, 18))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((1, 1, 16, 16), radius=3, fill='#29222e', outline=color)
    if kind == 'attack':
        draw.polygon([(12,3),(15,3),(15,6),(8,13),(5,10)], fill='#fff3ca')
        draw.line([(4,9),(9,14)], fill=color, width=2)
        draw.line([(6,12),(3,15)], fill='#ae6543', width=2)
    else:
        draw.polygon([(9,3),(14,5),(13,11),(9,15),(5,11),(4,5)], fill=color)
        draw.polygon([(9,5),(12,6),(11,10),(9,12)], fill='#d8f4ff')
    image.save(target / f'shrine_{kind}.png')
print('Generated two original shrine blessing effect icons')
