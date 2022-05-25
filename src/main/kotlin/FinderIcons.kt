import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO


object FinderIcons {
    val main: BufferedImage
        get() = getImage("iVBORw0KGgoAAAANSUhEUgAAAEAAAABBCAYAAABhNaJ7AAAACXBIWXMAAAsTAAALEwEAmpwYAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAboSURBVHgB7ZtbbBRVHMa/M7PbnRbbnm3XQim207gFVKKgVapiiyReYkzUqBhjEH1ReTK8+iImvho1mqiJUWNMTIgKJmpQI6G0CLSAlUu4tXRaqbS1l9PSdu9znLPbwm5D9zo7s1p+yWbnsjNz/t/5n++cMzNLEAfdtZFKkfBWQvja+O2EEyp2Iw8QDhpRJATqnV3RdRA19UFgnHOGDDBi6CNc6rp8+y+7E081S+V3zVuh6++RPAWajEi5jFC1E5ZAsDscdGz3r9ujxVYNPN9ueBMcO2ATgZtd4E4Cq+AgWiQkPyhEkOiuJtXO4EXtWxm8gICrjqLwu2JZkkPyDthI2OOALXA8qfyxUZUkIrXAJuyo/Xhk2blR0gm33PTmEALYidHbqZIdri/QXRL0Egl2k3YJggNuTLbegpkTN4GHcq+5SIU5tR85QhH4uo7p/UoXsiAtAXRfEXyna6LfwYEKBPo8yAUumZP+vLcYofal4EMKDX5Xp/JA5hmVpgBO8PDVAkcmi5ELeqk5tc8vuuJHgxQTsoYMyaoRxouRDWaZX2TapSVsCEgZ+5nlLiS6PbPMj0SQMGdB+D8gQCE4fzyWl8buvn8+1jcBxb6R37WwVACR/lwyUQATkslSAUyvfZeeeP4soknrEOJIvJBc6kM26A5zBZCqfVfGAVJxEMQdRqakJYBc5oPiHYotG8G71BFkA1fMTThp9RTk1YwRZ5g5mkZBykLIlLQn44p3MPrJhTwYIHU+KipmCNlimQeI9mmqAZqEdQIohTUAmsPCDCi82hdYVy1FKEiuZwAsws6bn8m4LgAsghdmJwDrnkrIC2cA0bkxT55dDvHEffPWE8+Z6C1RkYtIRn5jmQCOoXBs9haJBTwXdNIAc0CMOoUQ0Y9Crq4riQJZJoA8FYGVED8XzwBjK1OJ++aEgM7LbXowZy/RDJzh4vngRIFak3VcFwCLnEUvQFomaNgFa6iqpTV0KRSngk7tOMamJ1BorFm+Cs3e9eifuMgO9hyho1OTKY9JKUBFiZtta95CK5ZQ+MMz8Ed88IcCaD3XgUJjMjTOjMqiD3rvZ/fW30U/av8cF0eS374jnm82LDgSIRLHGw+/DhH8uG8Mb//8AUOE2PZCRTqscFezreufpbNlZm//9OHC5eV4K6kH3LZsdTT4w32dzF1cgc13PI5CRTTTTaub2PZNr4DIOvaeOyDKTN1LktdX0ibgC/mj3z+c+I16q+q19fV3qRO+Kfx8uhWFRGVpOXtu3RP05htVCCnOD/Z3nRnsXttQVc/Gp1lSBeSSzbU7Fto5PjOBk5fORg1vaGIUjXW3K+Ii5cVL2KlL5/3GYFKBjYgm2tJwD164+xmlqjT20sbYDMMXB3cqw5dHlUO9x1KVrzWlCf7NYrfCzw1foD3/aBACNNU30jpPDT47sNO23mDVMpU9fedjrLLYo85tE8F/tP9Lkblp+1RSE5yPp6wU2ze9CkUuubKtveew9v3Jn1Q9nP+nvqKdez119JFbWxBL96vMBT82ncErxIYJJm0C85kJBOGQnJr3RvWKwrUVK2hj7Tqjc/Cz6YBf8RldpNmIVL+l2qu98sDz/hbvfVGHj2d0mrGP275UMgo+RmtGGTDHaw9sMQZG9ddMs6MDR3G8rxunhs4wrufWZQpza1IbIfr0Yue1m/PFyQH2yf6v6EwgC+GNDMhKgBJXEba1vIjlpcuT/u7U4GkMGB4yMDaMXtZjZFAIhigL/l4EWeOuwqqlDWzlMhUrymqSCnis/4T2VecumvW7jtkKIKgoodjW/CIqlqR/bdGt+oP+6Pe4fzw6qhSUu8qEmaXss+P58dSvbO+Zg7kNynIRQFB5Qzle27A1IxFyZdw/hp2dP7Jzw725XzTVSDAVo1MTUecdnR7L2H2yoeOvDvbOL5+aE/wsOWXAlZNIOh5Z04yHGjYhH5we7NZ2/7kHI1NjKszEyABT7glyXcKe4+042t+FLY3PsZrympxrSMw823oPoe1shzYdCKrIE6ZkwHy8S2txd91arKleCcVRktGxwiDbeg6irbsDWXVtmcD59rzcFe4e6o9+jAEMW7N8Ja1117Jadw31lJYbgriYIUpChjCjR7gw0qd19p6gZ4d7YdUr/NwoX14yICXGhSVJpzzk0BSXi/qCfuu6kTg40V+257mAMULUdVn8mU81goedXL8rTDjRsIiROOEaFilElzRjoknexyKEc66NPNu+Txp9pk38mXgfFhHixkqE86fEctQEw7JDrOzDIkAETzieYpt/n/23ehyVO+97iUjS68biWvxPEAEbRhedrOmEfx8Jhd9jzx/S5van9S4J/XwjBc3TYIUpjL28z5LZ5LX4F57AssObkJwYAAAAAElFTkSuQmCC")

    private fun getImage(base64: String): BufferedImage {
        val image = Base64.getDecoder().decode("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")
        val bais = ByteArrayInputStream(image)
        return ImageIO.read(bais)
    }
}